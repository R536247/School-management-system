import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Students from './pages/Students'
import Employees from './pages/Employees'
import Attendance from './pages/Attendance'
import Reports from './pages/Reports'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login/>} />
      <Route path="/" element={<ProtectedRoute><Layout/></ProtectedRoute>}>
        <Route index element={<Navigate to="/dashboard" replace/>} />
        <Route path="dashboard" element={<Dashboard/>} />
        <Route path="students" element={<Students/>} />
        <Route path="employees" element={<Employees/>} />
        <Route path="attendance" element={<Attendance/>} />
        <Route path="reports" element={<Reports/>} />
      </Route>
    </Routes>
  )
}
