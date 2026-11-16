package com.example.desicalc.ui.strings

import com.example.desicalc.model.AppLanguage

data class DesiStrings(
    val navHome: String,
    val navCalc: String,
    val navKhata: String,
    val navAi: String,
    val navSmsReports: String,
    val navProfile: String,
    val totalCustomers: String,
    val totalUdhar: String,
    val totalJama: String,
    val remainingBalance: String,
    val todaysTransactions: String,
    val recentHistory: String,
    val pendingPayments: String,
    val quickCalculator: String,
    val askAiHint: String,
    val addCustomer: String,
    val searchCustomers: String,
    val udharDiya: String,
    val jamaReceived: String,
    val udharLiya: String,
    val paymentMade: String,
    val sendReminder: String,
    val confirmAndSave: String
)

fun localizedStrings(language: AppLanguage): DesiStrings {
    return when (language) {
        AppLanguage.HINDI -> DesiStrings(
            navHome = "होम",
            navCalc = "कैलकुलेटर",
            navKhata = "खाता",
            navAi = "AI स्मार्ट",
            navSmsReports = "SMS/रिपोर्ट",
            navProfile = "प्रोफ़ाइल",
            totalCustomers = "कुल ग्राहक",
            totalUdhar = "कुल उधार",
            totalJama = "कुल जमा",
            remainingBalance = "शेष बकाया",
            todaysTransactions = "आज का लेनदेन",
            recentHistory = "हालिया इतिहास",
            pendingPayments = "बकाया भुगतान",
            quickCalculator = "त्वरित कैलकुलेटर",
            askAiHint = "हिंदी या अंग्रेज़ी में पूछें (जैसे: राहुल को 500 उधार दिए)...",
            addCustomer = "नया ग्राहक जोड़ें",
            searchCustomers = "नाम, फ़ोन या ID से खोजें...",
            udharDiya = "उधार दिया",
            jamaReceived = "जमा मिला",
            udharLiya = "उधार लिया",
            paymentMade = "भुगतान किया",
            sendReminder = "रिमाइंडर भेजें",
            confirmAndSave = "पुष्टि करें और सहेजें"
        )
        AppLanguage.HINGLISH -> DesiStrings(
            navHome = "Home",
            navCalc = "Calc",
            navKhata = "Khata",
            navAi = "AI Smart",
            navSmsReports = "SMS/Report",
            navProfile = "Profile",
            totalCustomers = "Kul Customers",
            totalUdhar = "Total Udhar",
            totalJama = "Total Jama",
            remainingBalance = "Baki Balance",
            todaysTransactions = "Aaj ka Hisab",
            recentHistory = "Recent History",
            pendingPayments = "Pending Baki",
            quickCalculator = "Quick Calc",
            askAiHint = "Hindi/English mein likhein (e.g. Rahul ko 500 udhar diye)...",
            addCustomer = "Naya Customer",
            searchCustomers = "Naam, mobile ya ID search karein...",
            udharDiya = "Udhar Diya",
            jamaReceived = "Jama Mila",
            udharLiya = "Udhar Liya",
            paymentMade = "Payment Diya",
            sendReminder = "Reminder Bhejein",
            confirmAndSave = "Confirm & Save"
        )
        AppLanguage.ENGLISH -> DesiStrings(
            navHome = "Home",
            navCalc = "Calculator",
            navKhata = "Khata",
            navAi = "AI Smart",
            navSmsReports = "SMS & Reports",
            navProfile = "Profile",
            totalCustomers = "Total Customers",
            totalUdhar = "Total Udhar",
            totalJama = "Total Jama",
            remainingBalance = "Remaining Balance",
            todaysTransactions = "Today's Entries",
            recentHistory = "Recent History",
            pendingPayments = "Pending Payments",
            quickCalculator = "Quick Calculator",
            askAiHint = "Ask in Hindi or English (e.g. Rahul ko 500 rupaye udhar diye)...",
            addCustomer = "Add Customer",
            searchCustomers = "Search by name, phone, or ID...",
            udharDiya = "Udhar Diya",
            jamaReceived = "Jama Received",
            udharLiya = "Udhar Liya",
            paymentMade = "Payment Made",
            sendReminder = "Send Reminder",
            confirmAndSave = "Confirm & Save"
        )
    }
}
