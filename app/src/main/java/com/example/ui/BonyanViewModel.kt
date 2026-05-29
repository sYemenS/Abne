package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BonyanRepository
import com.example.data.Donation
import com.example.data.Project
import com.example.data.UserWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.InvalidParameterException

class BonyanViewModel(private val repository: BonyanRepository) : ViewModel() {

    // Active User Role state: "citizen" (المواطنون), "donor" (المتبرعون), "contractor" (المقاولون)
    val activeRole = MutableStateFlow("citizen")

    // Reactive streams from local database
    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDonations: StateFlow<List<Donation>> = repository.allDonations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allWallets: StateFlow<List<UserWallet>> = repository.allWallets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Convenient state helpers
    val donorWalletBalance: StateFlow<Double> = allWallets
        .combine(activeRole) { wallets, _ ->
            wallets.find { it.role == "donor" }?.balance ?: 150000.0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150000.0)

    val contractorWalletBalance: StateFlow<Double> = allWallets
        .combine(activeRole) { wallets, _ ->
            wallets.find { it.role == "contractor" }?.balance ?: 250000.0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 250000.0)

    // Layout helper states
    val selectedGridCoordinate = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentSelectedProject = MutableStateFlow<Project?>(null)

    // UI Message Notifications
    val successMessage = MutableStateFlow<String?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    fun changeRole(role: String) {
        activeRole.value = role
        // Deselect details on role swap to refresh list contextual views
        currentSelectedProject.value = null
        selectedGridCoordinate.value = null
    }

    fun reportNewProject(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        requiredAmount: Double,
        reporterName: String
    ) {
        viewModelScope.launch {
            try {
                if (title.isBlank() || description.isBlank() || reporterName.isBlank()) {
                    errorMessage.value = "يرجى تعبئة كافة الحقول المطلوبة!"
                    return@launch
                }
                if (requiredAmount <= 1000) {
                    errorMessage.value = "يرجى تحديد تكلفة واقعية للمشروع (أكثر من 1,000 ريال)!"
                    return@launch
                }

                repository.reportNewProject(
                    title = title,
                    description = description,
                    category = category,
                    latitude = latitude,
                    longitude = longitude,
                    requiredAmount = requiredAmount,
                    reporterName = reporterName
                )
                successMessage.value = "تم رصد المطلب الأهلي ورفع البلاغ الجغرافي بنجاح! سيتم فتح باب التبرع فوراً."
                selectedGridCoordinate.value = null
            } catch (e: Exception) {
                errorMessage.value = "خطأ أثناء إضافة البلاغ: ${e.message}"
            }
        }
    }

    fun donateToProject(projectId: Int, donorName: String, amount: Double) {
        viewModelScope.launch {
            try {
                if (donorName.isBlank()) {
                    errorMessage.value = "يرجى كتابة اسم المتبرع/الجهة!"
                    return@launch
                }
                if (amount <= 0) {
                    errorMessage.value = "يرجى تحديد مبلغ التبرع!"
                    return@launch
                }

                val result = repository.donateToProject(projectId, donorName, amount)
                if (result) {
                    successMessage.value = "جزاك الله خيراً! تم التبرع بمبلغ $amount ريال للمشروع المختار وتم تحديث شريط الإنجاز."
                    // Refresh localized details
                    val projects = allProjects.value
                    currentSelectedProject.value = projects.find { it.id == projectId }
                } else {
                    errorMessage.value = "فشل تنفيذ التبرع. قد يكون المشروع قد اكتمل تمويله بالفعل!"
                }
            } catch (e: InvalidParameterException) {
                errorMessage.value = e.message
            } catch (e: Exception) {
                errorMessage.value = "خطأ غير متوقع: ${e.message}"
            }
        }
    }

    fun claimProject(projectId: Int, contractorName: String) {
        viewModelScope.launch {
            try {
                if (contractorName.isBlank()) {
                    errorMessage.value = "يرجى إدخال اسم المقاول أو اسم الشركة الهندسية!"
                    return@launch
                }
                val result = repository.claimProject(projectId, contractorName)
                if (result) {
                    successMessage.value = "تم إسناد المشروع لك بنجاح ودفع مبلغ التأمين (80% ضمان مالي) لحساب الضمان الآمن."
                    // Refresh detail layout
                    val projects = allProjects.value
                    currentSelectedProject.value = projects.find { it.id == projectId }
                } else {
                    errorMessage.value = "فشل حجز المشروع. تأكد من توفر شروط الاستلام والموازنة!"
                }
            } catch (e: InvalidParameterException) {
                errorMessage.value = e.message
            } catch (e: Exception) {
                errorMessage.value = "حدث خطأ: ${e.message}"
            }
        }
    }

    fun completeProject(projectId: Int) {
        viewModelScope.launch {
            try {
                val result = repository.completeProject(projectId)
                if (result) {
                    successMessage.value = "تهانينا! تم استلام تقرير التدقيق، وإكمال المشروع بنجاح وصرف مستحقاتك (قيمة المشروع كاملة + استرداد الضمان المالي 80%)."
                    val projects = allProjects.value
                    currentSelectedProject.value = projects.find { it.id == projectId }
                } else {
                    errorMessage.value = "فشلت عملية إتمام المشروع!"
                }
            } catch (e: Exception) {
                errorMessage.value = "حدث خطأ: ${e.message}"
            }
        }
    }

    fun forfeitProject(projectId: Int) {
        viewModelScope.launch {
            try {
                val result = repository.forfeitProject(projectId)
                if (result) {
                    errorMessage.value = "تنبيه عقابي: تم إلغاء العقد ومصادرة كامل مبلغ الضمان المالي (80%) وتحويله لدعم المشاريع الأهلية لإخلالك بالشروط."
                    val projects = allProjects.value
                    currentSelectedProject.value = projects.find { it.id == projectId }
                } else {
                    errorMessage.value = "فشلت عملية إلغاء العقد!"
                }
            } catch (e: Exception) {
                errorMessage.value = "حدث خطأ: ${e.message}"
            }
        }
    }

    fun clearMessages() {
        successMessage.value = null
        errorMessage.value = null
    }
}

class BonyanViewModelFactory(private val repository: BonyanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BonyanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BonyanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
