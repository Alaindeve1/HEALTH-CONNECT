# 📄 Telehealth & Remote Consultation Hub – SRS

**Version:** 2.0  
**Date:** 26-Nov-2025

# 📘 Simplified Software Requirements Specification (SRS)
## Telehealth & Remote Consultation App
*A short and easy-to-understand SRS*

---

# 1. Introduction

The Telehealth & Remote Consultation App helps **Community Health Workers (CHWs)** connect with **Doctors** through real-time chat or video for remote medical support. CHWs can register patients, record vitals, and request consultations. Doctors can review patient data, chat with CHWs, and make clinical decisions.

The app supports:
- Real-time communication  
- Vitals & symptoms sharing  
- Patient registration  
- Offline data saving + auto sync  
- Push notifications  
- FHIR standard for all health data  

Users:
- **CHW**
- **Doctor**
- **Admin**

---

# 2. System Overview

### Main Features
- Patient creation & management (CHW, Doctor)
- Vitals and symptoms recording
- Consultation request workflow
- Chat communication between CHW & Doctor
- Optional video call integration
- Offline-first functionality
- FHIR-based medical record storage
- Background sync for all offline data

### Technologies
- Android (Java)
- Local SQLite database (offline)
- FHIR backend server
- Push notification system

---

# 3. Functional Requirements (Summary)

## CHW
- Register and manage patient records  
- Record vitals (temperature, BP, HR, RR)  
- Add symptoms  
- Request consultation  
- Chat with Doctor  
- Sync offline data  
- View history  

## Doctor
- Login  
- View consultation requests  
- Accept/decline requests  
- Chat with CHW  
- View vitals & patient data  
- Provide notes/actions  

## Admin
- Manage users  
- Assign system roles  

---

# 4. Non-Functional Requirements

- **Usability:** Interface simple enough for field workers  
- **Performance:** Runs smoothly on low-end Android devices  
- **Security:** HTTPS and encrypted local storage  
- **Offline-first:** All CHW functions must work offline  
- **Scalability:** Supports many CHWs and doctors  

---

# 5. System Diagrams

## 5.1 Use Case Diagram  
![Use Case Diagram](media/telehealth_usecase.png)

---

## 5.2 Class Diagram  
![Class Diagram](media/telehealth_class.png)

---

## 5.3 Data Flow Diagram – Level 0  
![DFD Level 0](media/telehealth_dfd0.png)

---

## 5.3 Data Flow Diagram – Level 1  
![DFD Level 1](media/telehealth_dfd1.png)

---

# 6. Glossary

| Term | Description |
|------|-------------|
| **CHW** | Community Health Worker |
| **FHIR** | Health data standard |
| **Observation** | Recorded vital signs |
| **ServiceRequest** | Consultation request |
| **Encounter** | Consultation session |
| **Communication** | Chat messages |

---
# 7. UI Design

The following are sample screens from the **Telehealth & Remote Consultation Hub** application. Each screen highlights core functionalities and demonstrates the user interface design direction.

| Screen                                               | Screen                                                  |
| ---------------------------------------------------- | ------------------------------------------------------- |
| ![UI 1](media/telehealth_ui_1.png) <br> **Screen 1** | ![UI 2](media/telehealth_ui_2.png) <br> **Screen 2**    |
| ![UI 3](media/telehealth_ui_3.png) <br> **Screen 3** | ![UI 4](media/telehealth_ui_4.png) <br> **Screen 4**    |
| ![UI 5](media/telehealth_ui_5.png) <br> **Screen 5** | ![UI 6](media/telehealth_ui_6.png) <br> **Screen 6**    |
| ![UI 7](media/telehealth_ui_7.png) <br> **Screen 7** | ![UI 8](media/telehealth_ui_8.png) <br> **Screen 8**    |
| ![UI 9](media/telehealth_ui_9.png) <br> **Screen 9** | ![UI 10](media/telehealth_ui_10.png) <br> **Screen 10** |

# 8. Conclusion

This simplified SRS provides a clear overview of how the Telehealth system works, key features, user roles, and the data flow. It gives the team everything needed to begin implementation while being easy to understand and follow.
