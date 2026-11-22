INSERT INTO skills(name) VALUES ('Applications'),('Database'),('Network'),('Hardware'),('Identity'),('Cloud');
INSERT INTO services(name,criticality,required_skill) VALUES ('Payment Gateway',3,'Applications'),('Corporate Network',3,'Network'),('Employee Portal',2,'Identity'),('Cloud Storage',2,'Cloud'),('Workstation',1,'Hardware'),('Data Platform',3,'Database');
INSERT INTO incident_categories(name,required_skill) VALUES ('Application failure','Applications'),('Database issue','Database'),('Network connectivity','Network'),('Hardware malfunction','Hardware'),('Access & authentication','Identity'),('Cloud services','Cloud');
INSERT INTO sla_policies(priority,response_minutes,resolution_minutes) VALUES ('P1',5,30),('P2',15,120),('P3',60,480),('P4',240,1440);
INSERT INTO system_settings VALUES(1,'SKILL','WEIGHTED',24,12,5,TRUE);
