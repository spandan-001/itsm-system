--Contains Sample Data (test records) for testing the database


INSERT INTO employees (name, email, password_hash, department) VALUES
    ('Sample Employee 1', 'sample.emp1@example.test', 'placeholder_hash_emp1', 'Operations'),
    ('Sample Employee 2', 'sample.emp2@example.test', 'placeholder_hash_emp2', 'Finance');

INSERT INTO technicians (name, email, password_hash, domain, available) VALUES
    ('Sample Technician 1', 'sample.tech1@example.test', 'placeholder_hash_tech1', 'NETWORK', TRUE),
    ('Sample Technician 2', 'sample.tech2@example.test', 'placeholder_hash_tech2', 'HARDWARE', TRUE);

INSERT INTO it_services (name, description, criticality, active) VALUES
    ('Campus Wi-Fi', 'Main campus wireless network', 'HIGH', TRUE),
    ('Student Portal', 'Web portal for students', 'MEDIUM', TRUE),
    ('Printing Service', 'Shared campus printers', 'LOW', TRUE);
