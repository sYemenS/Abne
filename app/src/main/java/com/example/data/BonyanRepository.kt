package com.example.data

import kotlinx.coroutines.flow.Flow
import java.security.InvalidParameterException

class BonyanRepository(private val dao: BonyanDao) {
    val allProjects: Flow<List<Project>> = dao.getAllProjects()
    val allDonations: Flow<List<Donation>> = dao.getAllDonations()
    val allWallets: Flow<List<UserWallet>> = dao.getAllWalletsFlow()

    suspend fun getWalletBalance(role: String): Double {
        return dao.getWalletByRole(role)?.balance ?: 0.0
    }

    suspend fun updateWalletBalance(role: String, newBalance: Double) {
        dao.insertWallet(UserWallet(role, newBalance))
    }

    suspend fun reportNewProject(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        requiredAmount: Double,
        reporterName: String
    ): Long {
        val proj = Project(
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            requiredAmount = requiredAmount,
            collectedAmount = 0.0,
            status = "قيد_التمويل",
            reporterName = reporterName
        )
        return dao.insertProject(proj)
    }

    // Donor process
    suspend fun donateToProject(projectId: Int, donorName: String, amount: Double): Boolean {
        if (amount <= 0) return false
        val project = dao.getProjectById(projectId) ?: return false
        if (project.status != "قيد_التمويل") return false

        val donorWallet = dao.getWalletByRole("donor") ?: UserWallet("donor", 150000.0)
        if (donorWallet.balance < amount) {
            throw InvalidParameterException("رصيد محفظة المانح غير كافٍ لإتمام التبرع!")
        }

        // Calculate actual usable donation (cap at remaining required)
        val remainingNeeded = project.requiredAmount - project.collectedAmount
        val actualDonation = if (amount > remainingNeeded) remainingNeeded else amount
        if (actualDonation <= 0) return false

        // Update Project
        val updatedCollected = project.collectedAmount + actualDonation
        val newStatus = if (updatedCollected >= project.requiredAmount) "مكتمل_التمويل" else "قيد_التمويل"
        val updatedProject = project.copy(
            collectedAmount = updatedCollected,
            status = newStatus
        )
        dao.updateProject(updatedProject)

        // Update Wallet
        dao.insertWallet(donorWallet.copy(balance = donorWallet.balance - actualDonation))

        // Create transaction log
        dao.insertDonation(
            Donation(
                projectId = projectId,
                projectTitle = project.title,
                donorName = donorName,
                amount = actualDonation
            )
        )
        return true
    }

    // Contractor Claim system (Escrow 80%)
    suspend fun claimProject(projectId: Int, contractorName: String): Boolean {
        val project = dao.getProjectById(projectId) ?: return false
        if (project.status != "مكتمل_التمويل") return false

        val requiredEscrow = project.requiredAmount * 0.80
        val contractorWallet = dao.getWalletByRole("contractor") ?: UserWallet("contractor", 250000.0)

        if (contractorWallet.balance < requiredEscrow) {
            throw InvalidParameterException("رصيد محفظتك الحالي لا يغطي مبلغ الضمان المالي المطلوب (80% يعادل ${requiredEscrow.toInt()} ريال)")
        }

        // Deduct Escrow guarantee from contractor balance
        val newBalance = contractorWallet.balance - requiredEscrow
        dao.insertWallet(contractorWallet.copy(balance = newBalance))

        // Assign status to In Progress (قيد_التنفيذ)
        val updatedProject = project.copy(
            status = "قيد_التنفيذ",
            contractorName = contractorName,
            escrowDeposited = requiredEscrow
        )
        dao.updateProject(updatedProject)
        return true
    }

    // Contractor Complete project (pays whole budget 100% + returns the 80% back!)
    suspend fun completeProject(projectId: Int): Boolean {
        val project = dao.getProjectById(projectId) ?: return false
        if (project.status != "قيد_التنفيذ") return false

        val contractorWallet = dao.getWalletByRole("contractor") ?: UserWallet("contractor", 250000.0)

        // Payout: Return 80% guarantee AND pay out 100% contract budget
        val payout = project.requiredAmount + project.escrowDeposited
        val newBalance = contractorWallet.balance + payout
        dao.insertWallet(contractorWallet.copy(balance = newBalance))

        // Progress project to completed
        val updatedProject = project.copy(
            status = "مكتمل_التنفيذ"
        )
        dao.updateProject(updatedProject)
        return true
    }

    // Contractor cancels contract / Penalty (Escrow 80% forfeited!)
    suspend fun forfeitProject(projectId: Int): Boolean {
        val project = dao.getProjectById(projectId) ?: return false
        if (project.status != "قيد_التنفيذ") return false

        // Contractor loses their deposited escrow. The project is reset back to "مكتمل_التمويل" (Ready for claims)
        // for other contractors to pick up, and contractor assignment is wiped.
        val updatedProject = project.copy(
            status = "مكتمل_التمويل",
            contractorName = null,
            escrowDeposited = 0.0
        )
        dao.updateProject(updatedProject)
        return true
    }
}
