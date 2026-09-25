import React from 'react'
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'
import ReservationListPage from './features/reservations/ReservationListPage.jsx'
import SlotBookingPage from './features/reservations/SlotBookingPage.jsx'

function NavBar() {
  return (
    <nav style={{
      background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
      padding: '0 2rem',
      display: 'flex',
      alignItems: 'center',
      gap: '2rem',
      height: '60px',
      borderBottom: '1px solid #334155',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <span style={{ color: '#f59e0b', fontWeight: 700, fontSize: '1.1rem' }}>
        ☀️ Smart Solar Microgrid
      </span>
      <Link to="/reservations" style={{ color: '#94a3b8', textDecoration: 'none', fontSize: '0.9rem' }}>
        Reservations
      </Link>
      <Link to="/book-slot" style={{ color: '#94a3b8', textDecoration: 'none', fontSize: '0.9rem' }}>
        Book Slot
      </Link>
    </nav>
  )
}

function App() {
  return (
    <BrowserRouter>
      <NavBar />
      <Routes>
        <Route path="/" element={<Navigate to="/reservations" replace />} />
        <Route path="/reservations" element={<ReservationListPage />} />
        <Route path="/book-slot" element={<SlotBookingPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
