
import './MyIncidents.css';
import { Link } from 'react-router-dom';

function MyIncidents() {

  const incidents = JSON.parse(
    localStorage.getItem('itsmIncidents') || '[]'
  );

  return (
    <div className="my-incidents">

      <Link to="/employee" className="back-link">
        ← Back to Dashboard
      </Link>

      <h1>My Incidents</h1>
      <p>View and track the IT issues you have reported.</p>

      <div className="incidents-table">

        {incidents.length === 0 ? (
          <p>You haven't reported any incidents yet.</p>
        ) : (

          <table>
            <thead>
              <tr>
                <th>Incident ID</th>
                <th>Issue</th>
                <th>Category</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>

            <tbody>
              {incidents.map((incident) => (
                <tr key={incident.id}>
                  <td>{incident.id}</td>
                  <td>{incident.title}</td>
                  <td>{incident.category}</td>
                  <td>{incident.priority}</td>
                  <td>{incident.status}</td>
                  <td>{incident.date}</td>
                </tr>
              ))}
            </tbody>
          </table>

        )}

      </div>
    </div>
  );
}

export default MyIncidents;
