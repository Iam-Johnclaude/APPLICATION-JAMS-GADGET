Jams Gadget Inventory 📱💻

About the Application

Jams Gadget Inventory is a specialized Retail & Credit Management System designed for gadget businesses that offer flexible payment plans. Unlike a simple inventory app, this system bridges the gap between Stock Management and Customer Debt Tracking.

It is built to help business owners move away from manual record-keeping by providing a real-time dashboard of who owes money, how much is outstanding across the entire business, and which payments are overdue.

Core Features
•Live Installment Calculator: Automatically computes monthly amortization, balances, and due dates as you type.
•Smart Item Picker: Links installment plans directly to your existing gadget stock with a searchable bottom-sheet interface.
•Financial Health Dashboard: View your "Total Outstanding Balance" and status counters (Active/Overdue/Paid) at a glance.
•Cloud Synchronization: Powered by Firebase, ensuring your data is accessible on any device and backed up instantly.
•Automated Status Tracking: Intelligent logic flags "Overdue" accounts automatically based on payment dates.
•Professional UI: Includes a dedicated "Navy & Gold" Dark Mode and smooth animations for a premium business feel.

How to Use the Application
1. Managing Your Dashboard
When you open the app, you are greeted with the Installments Dashboard.
•The Top Summary: Shows the total money currently "on the street" (uncollected debt).
•Status Tabs: Use the "Pill" filters to switch between Active (paying), Overdue (late), and Paid (completed) customers.

2. Adding a New Installment (Sale)
  1.Tap the Floating Action Button (+) on the main screen.
  2.Select an Item: Tap "Select Item" to open your inventory. Use the search bar or category chips (Smartphones, Accessories, etc.) to find the gadget.
  3.Enter Customer Info: Fill in the name, contact number, and address.
  4.Set Payment Terms:
    ◦Enter the Total Price.
    ◦Enter the Down Payment (the app will instantly show the remaining balance).
    ◦Choose the Duration (1 to 12 months). The app will automatically calculate the Monthly Payment.
  5.Save: Tap "Save Installment." The record is now live and synced.

3. Tracking & Updating Payments
  1.Click on any customer in the list to open their Detail View.
  2.Review their balance and payment history.
  3.When a customer pays, update their Total Paid amount. If the balance reaches 0, the app automatically moves them to the Paid category.

4. Handling Overdue Accounts
Keep an eye on the Overdue tab. The app highlights these records in red. You can use the contact information stored in their profile to reach out via phone or email directly from your device.

Technical Requirements
•Android OS: Version 7.0 (Nougat) or higher.
•Internet: Required for real-time Firebase synchronization.
•Permissions: Camera (for product photos) and Internet access.



Developer Setup
1.Clone this repository. 
2.Add your google-services.json file from Firebase to the /app folder.
3.Enable Firestore Database and Firebase Authentication in your console.
4.Build and run using Android Studio.











  
