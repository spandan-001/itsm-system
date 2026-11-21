
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ReportIncident from './pages/ReportIncident';
import MyIncidents from './pages/MyIncidents';

import Login from './pages/Login';
import EmployeeDashboard from './pages/EmployeeDashboard';
import TechnicianDashboard from './pages/TechnicianDashboard';
import AdminDashboard from './pages/AdminDashboard';

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/" element={<Login />} />

        <Route path="/employee" element={<EmployeeDashboard />} />

        <Route path="/technician" element={<TechnicianDashboard />} />

        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/report-incident" element={<ReportIncident />} />
        <Route path="/my-incidents" element={<MyIncidents />} />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
