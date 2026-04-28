Jams Gadget Inventory 📱💻
A comprehensive Retail & Credit Management System built for Android. This application empowers gadget business owners to manage their inventory, track customer installments, and monitor the financial health of their business in real-time.

🚀 Key Features

1. Advanced Installment & Credit Tracking
•Live Compute Engine: Automatically calculates remaining balances and monthly amortizations as you input data.
•Smart Due-Date Logic: Automatically assigns recurring monthly due dates based on the initial sale date.
•Status Management: Categorize accounts into Active, Overdue, or Paid to prioritize collection efforts.
•Financial Dashboard: View total outstanding business balances at a glance.

2. Smart Inventory Integration
•Product Picker: Seamlessly link gadget stock to installment plans using a searchable bottom-sheet dialog.
•Category Normalization: Automatically groups products (e.g., "Smartphones", "Tablets") for a cleaner user experience.
•Visual Tracking: Displays product images, brands, and categories within transaction records.

3. Modern User Experience
•Adaptive Themes: Includes a premium "Navy & Gold" Dark Mode for professional use in any lighting.
•Real-Time Sync: Powered by Firebase, ensuring all data is instantly updated across multiple devices.
•Interactive Visuals: Uses MPAndroidChart to visualize sales trends and collection ratios.
•Android 12+ Splash Screen: A smooth, branded entry into the application.

🛠 Tech Stack
•Language: Java / Kotlin
•Database: Firebase Cloud Firestore (Real-time data storage)
•Authentication: Firebase Auth
•Image Loading: Glide (Optimized image caching and processing)
•UI Components: Material Design 3, CoordinatorLayout, ViewBinding
•Charts: MPAndroidChart
•Reminders: Integrated with Google Calendar API and Android Mail for automated notifications.

📸 Presentation Walkthrough
If you are evaluating this project for a presentation or demonstration, follow this recommended flow:
1.Dashboard: Show the "Total Outstanding Balance" to highlight business-level insights.
2.Creation: Open AddInstallmentActivity and demonstrate the Live Compute feature by changing the down payment.
3.Selection: Use the Item Picker to show how it pulls from the live inventory.
4.Filtering: Toggle between the "Active" and "Overdue" tabs to show how the app manages collection priority.

📦 Installation
1.Clone the repository:
Shell Script
git clone https://github.com/your-username/JamsGadgetInventory.git
2.Open the project in Android Studio.
3.Connect your own Firebase Project and download the google-services.json file into the app/ directory.
4.Sync the project with Gradle files.
5.Run on an emulator or physical device (API level 24+ recommended).

🤝 Contributing
Contributions are welcome! Feel free to open an issue or submit a pull request for new features or bug fixes.
⚖️ License
This project is developed for educational/business management purposes. [Choose your License, e.g., MIT]
