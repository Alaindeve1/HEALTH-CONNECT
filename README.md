## Telehealth & Remote Consultation Hub

### Description

An app facilitating real-time (video/chat) consultations between a Community Health Worker (CHW) in a remote village and a doctor at a district hospital. The CHW can share patient vitals and symptoms using the FHIR data within the session.

### Goal

To build a mobile app that connects a local health worker (CHW) in a village with a doctor at a main hospital for **real-time video or chat consultations**. The app uses FHIR to make sure all patient data shared is secure, organized, and saved as a medical record.

### How It Works (3 Simple Steps)

#### 1. Start the Session

- **Request:** The CHW uses the app to ask a doctor for a consult (creates a FHIR **`ServiceRequest`**).

- **Connection:** When the doctor accepts, the app starts a video or chat call and records the details of the session (FHIR **`Encounter`**).

### Description

#### 2. Share Data Live

- **Vitals Input:** During the call, the CHW enters the patient's vitals (like heart rate or temperature) into the app.

- **FHIR Data:** The app immediately converts this input into secure FHIR **`Observation`** resources and sends them to the server.

- **Doctor View:** The doctor sees the patient's data instantly update on their screen, helping them make a better decision.

- **Messaging:** Any chat messages are saved as FHIR **`Communication`** resources.


#### 3. Finish & Follow Up

- **Doctor's Orders:** The doctor uses the app to create a task for the patient (e.g., "Take this medication" or "Get a lab test") using a FHIR **`ServiceRequest`**.

- **Record:** The app closes the session by marking the FHIR **`Encounter`** as complete.


### Key FHIR Tools Used

- **`Observation`:** For sharing vitals and symptoms live.

- **`Communication`:** For saving the chat history.

- **`Encounter`:** For recording the consultation itself.

- **`ServiceRequest`:** For starting the consult and ordering follow-up actions.
