<div align="center">

# 🎓 UniFix
**Smart University Problem Reporting & Management System**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-%23039BE5.svg?style=for-the-badge&logo=firebase)
![Material Design](https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white)

*Bridging the gap between campus staff and students for a seamless university experience.*

</div>

---

## 📖 About The Project

**UniFix** is a comprehensive, multi-role Android application designed to streamline maintenance, ICT, and facility problem reporting across a university campus. By replacing outdated manual reporting methods with a real-time, digital workflow, UniFix ensures that issues are reported quickly, routed to the correct department accurately, and resolved efficiently.

---

## 👥 Who Will Use UniFix?

UniFix is built to support the entire university ecosystem. Every user has a tailored experience designed for their specific daily needs:

* **🎓 Students:** Can instantly report issues in their dormitories, cafeterias, or general campus facilities. They can upload photo evidence and track the exact status of their tickets in real time.
* **📚 Teachers:** Utilize a specialized reporting flow designed for academic needs, such as reporting broken classroom equipment (like projectors), requesting teaching materials, or filing office maintenance requests.
* **🔧 Solvers (Campus Technicians & Staff):** Receive automated, department-specific task assignments (e.g., ICT, Health, Maintenance). They can update task statuses to "In Progress" or "Finished," delegate tasks to other staff, or appeal assignments directly to the Administration.
* **👑 Administrators:** The command center of the campus. Admins oversee all operations, manage user accounts (warn/ban/restore), review staff appeals, and utilize real-time analytics to monitor resolution times and department performance.

---

## 📱 Application Dashboards

<div align="center">
  <table>
    <tr>
      <td align="center"><b>🔐 Login & Auth</b></td>
      <td align="center"><b>🎓 Student Dashboard</b></td>
    </tr>
    <tr>
      <td align="center">
        <img src="screenshots/login.png" width="250" alt="Login Screen"/>
      </td>
      <td align="center">
        <img src="screenshots/student_dash.png" width="250" alt="Student Dashboard"/>
      </td>
    </tr>
    <tr>
      <td align="center"><i>Secure access with ML-based ID card verification.</i></td>
      <td align="center"><i>Submit issues, upload evidence, and track progress.</i></td>
    </tr>
  </table>

  <br>

  <table>
    <tr>
      <td align="center"><b>🔧 Solver Dashboard</b></td>
      <td align="center"><b>👑 Admin Dashboard</b></td>
    </tr>
    <tr>
      <td align="center">
        <img src="screenshots/solver_dash.png" width="250" alt="Solver Dashboard"/>
      </td>
      <td align="center">
        <img src="screenshots/admin_dash.png" width="250" alt="Admin Dashboard"/>
      </td>
    </tr>
    <tr>
      <td align="center"><i>Manage active tasks and dynamic deadlines.</i></td>
      <td align="center"><i>Campus analytics, user management, and exports.</i></td>
    </tr>
  </table>
</div>

*(Note: Teacher Dashboard layout is included in the app but not pictured above).*

---

## ✨ Core Features

* **🔐 Role-Based Access Control:** Secure, customized environments tailored to the four core user types.
* **🆔 Smart ID Verification:** Utilizes **Google ML Kit** (OCR & Barcode scanning) to automatically verify physical Student and Teacher ID cards during registration.
* **🌍 Bilingual Support:** Real-time UI translation toggling between **English** and **Amharic (አማርኛ)**.
* **🌗 Adaptive UI:** Premium interface with dynamic Light and Dark mode toggling that respects system settings.
* **🛎️ Real-Time System Alerts:** A global notification bell system that alerts users when tasks are assigned, appealed, or resolved.
* **💬 Communication Center:** Integrated private messaging and ticket-specific Group Chats for seamless collaboration.
* **📊 Analytics & Export:** Admins have access to real-time interactive charts and can export campus performance reports directly to **PDF** and **CSV**.
* **⏳ Dynamic Deadlines:** Automated tracking of ticket urgency (Urgent, High, Medium, Low) with visual overdue warnings.

---

## 🛠️ Technology Stack

* **Frontend:** Java, Android XML, Material Components
* **Backend & Database:** Firebase Cloud Firestore
* **Storage:** Firebase Storage (for issue attachments and user data)
* **Authentication:** Firebase Auth & Custom Role-Based Logic
* **Machine Learning:** Google ML Kit (Text Recognition API, Barcode Scanning API)
* **Libraries Used:** * `Glide` - Image loading and caching
    * `MPAndroidChart` - Beautiful, interactive data visualization
    * `android.graphics.pdf.PdfDocument` - Native PDF generation

---

## 📂 Project Architecture

```text
UniFix/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/UniFix/unifix/
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── AdminDashboardActivity.java
│   │   │   │   ├── SolverDashboardActivity.java
│   │   │   │   ├── StudentDashboardActivity.java
│   │   │   │   └── TeacherDashboardActivity.java
│   │   │   ├── res/
│   │   │   │   ├── layout/          # XML UI layouts
│   │   │   │   └── drawable/        # App assets, icons, backgrounds
│   │   │   └── AndroidManifest.xml
├── screenshots/                     # Project visual documentation
│   ├── login.png
│   ├── admin_dash.png
│   ├── solver_dash.png
│   └── student_dash.png
├── build.gradle
└── README.md