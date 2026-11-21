
import './ReportIncident.css';
import { Link, useNavigate } from 'react-router-dom';
import { assignTechnician } from '../data/technicians';
import { calculateSLADeadline } from '../data/sla';

function ReportIncident() {
  const navigate = useNavigate();

  function handleSubmit(event) {
    event.preventDefault();

    const form = event.currentTarget;

    const title = form.elements.title.value.trim();
    const description = form.elements.description.value.trim();
    const category = form.elements.category.value;
    const impact = form.elements.impact.value;
    const urgency = form.elements.urgency.value;

    // Load existing incidents
    const incidents = JSON.parse(
      localStorage.getItem('itsmIncidents') || '[]'
    );

    // Generate a unique incident ID
    const existingNumbers = incidents
      .map((incident) =>
        Number(String(incident.id).replace(/^INC/, ''))
      )
      .filter(Number.isFinite);

    const nextNumber =
      Math.max(0, ...existingNumbers) + 1;

    const newId =
      'INC' + String(nextNumber).padStart(3, '0');

    // Calculate priority
    let priority = 'Medium';

    if (impact === 'High' || urgency === 'High') {
      priority = 'High';
    } else if (impact === 'Low' && urgency === 'Low') {
      priority = 'Low';
    }

    // Create the incident
    const newIncident = {
      id: newId,
      title,
      description,
      category,
      impact,
      urgency,
      priority,
      status: 'Open',
      status: 'Open',
assignedTo: '',
createdAt: new Date().toISOString(),
resolvedAt: null,
escalationLevel: 0,
escalationHistory: [],
slaDeadline: calculateSLADeadline(
  priority,
  new Date().toISOString()
),
date: new Date().toLocaleDateString('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric'
})
    };

    // Automatic technician assignment
    const assignedTechnician = assignTechnician(
      newIncident,
      incidents
    );

    newIncident.assignedTo =
      assignedTechnician || '';

    // Save incident
    incidents.push(newIncident);

    localStorage.setItem(
      'itsmIncidents',
      JSON.stringify(incidents)
    );

    // Confirmation
    if (assignedTechnician) {
      alert(
        `Incident ${newId} submitted successfully!\n` +
        `Assigned to: ${assignedTechnician}`
      );
    } else {
      alert(
        `Incident ${newId} submitted successfully!\n` +
        'No suitable technician available. ' +
        'Admin assignment required.'
      );
    }

    navigate('/my-incidents');
  }

  return (
    <div className="report-page">

      <div className="report-header">
        <Link to="/employee" className="back-link">
          ← Back to Dashboard
        </Link>

        <h1>Report an Incident</h1>
        <p>
          Describe the IT issue you're experiencing.
        </p>
      </div>

      <form
        className="incident-form"
        onSubmit={handleSubmit}
      >

        <div className="form-group">
          <label htmlFor="incident-title">
            Incident Title
          </label>
          <input
            id="incident-title"
            type="text"
            name="title"
            placeholder="e.g. Unable to connect to Wi-Fi"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="incident-description">
            Description
          </label>
          <textarea
            id="incident-description"
            name="description"
            placeholder="Explain the issue in detail..."
            rows="5"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="incident-category">
            Category
          </label>
          <select
            id="incident-category"
            name="category"
            required
            defaultValue=""
          >
            <option value="" disabled>
              Select a category
            </option>
            <option>Hardware</option>
            <option>Software</option>
            <option>Network</option>
            <option>Email</option>
            <option>Other</option>
          </select>
        </div>

        <div className="form-row">

          <div className="form-group">
            <label htmlFor="incident-impact">
              Impact
            </label>
            <select
              id="incident-impact"
              name="impact"
              required
              defaultValue=""
            >
              <option value="" disabled>
                Select impact
              </option>
              <option>High</option>
              <option>Medium</option>
              <option>Low</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="incident-urgency">
              Urgency
            </label>
            <select
              id="incident-urgency"
              name="urgency"
              required
              defaultValue=""
            >
              <option value="" disabled>
                Select urgency
              </option>
              <option>High</option>
              <option>Medium</option>
              <option>Low</option>
            </select>
          </div>

        </div>

        <div className="form-group">
          <label htmlFor="incident-attachment">
            Attachments (Optional)
          </label>
          <input
            id="incident-attachment"
            type="file"
          />
          <small>
            You can attach a screenshot or document.
            File uploading will be connected later.
          </small>
        </div>

        <div className="form-actions">
          <Link
            to="/employee"
            className="cancel-button"
          >
            Cancel
          </Link>

          <button
            type="submit"
            className="submit-button"
          >
            Submit Incident
          </button>
        </div>

      </form>
    </div>
  );
}

export default ReportIncident;
