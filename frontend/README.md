# Frontend - React + Vite + Tailwind

Modern React-basert frontend for School Management System med fullt funksjonell admin-panel.

## Quick Start

```bash
cd frontend
npm install
npm run dev
```

Åpne http://localhost:5173

## Mappestruktur

```
frontend/src/
├── pages/               # Hele sider
│   ├── Login.jsx       # Innlogging
│   ├── Dashboard.jsx   # Dashboard med grafer
│   ├── Students.jsx    # Studentliste & CRUD
│   ├── Employees.jsx   # Ansattliste & CRUD
│   ├── Attendance.jsx  # Frammøtemarking
│   └── Reports.jsx     # Rapporter
├── components/         # Gjenbrukbare komponenter
│   ├── Layout.jsx      # Sidobar + topbar
│   ├── ProtectedRoute  # Route protection
│   ├── Button.jsx      # Knappkomponent
│   ├── Modal.jsx       # Dialog/modal
│   ├── Table.jsx       # Datatabell
│   ├── FormInputs.jsx  # Input/Select/TextArea/Checkbox
│   ├── Card.jsx        # Container
│   ├── Badge.jsx       # Status badges
│   ├── Alert.jsx       # Notifications
│   ├── Loading.jsx     # Spinner & skeleton
│   └── index.js        # Sentral eksport
├── services/
│   └── api.js          # Axios HTTP-klient
├── utils/
│   └── auth.js         # Token-håndtering
└── App.jsx             # Router

## Features

✅ **Fullt funksjonell admin-panel**
- Dashboard med grafer (Chart.js)
- CRUD for Studenter, Ansatte
- Frammøtemarking
- Rapportgenerator

✅ **Moderne UI**
- 10+ gjenbrukbare komponenter
- Tailwind CSS styling
- Lucide React ikoner
- Responsive design

✅ **Sikkerhet**
- JWT autentisering
- Protected routes
- Auto token-attach via interceptor

## Environment

```bash
VITE_API_URL=http://localhost:8080
```

## Build

```bash
npm run build        # Bygd for produksjon
npm run start        # Kjør preview
```

## Dependencies

- **react** 18.2.0
- **react-router-dom** 6.14.1
- **axios** 1.4.0
- **chart.js** 4.4.0 + react-chartjs-2 5.2.0
- **lucide-react** 0.268.0
- **tailwindcss** 3.4.7
- **vite** 5.1.0

