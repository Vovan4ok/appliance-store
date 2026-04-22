-- Passwords (all meet the new policy: 8+ chars, upper, lower, special):
-- Phobos  -> Phobos@123!
-- Moon    -> Moon@123!
-- Deimos  -> Deimos@123!
-- Europa  -> Europa@123!
INSERT INTO employee (name, email, password, department) VALUES
    ('Phobos','phobos@gmail.com','$2a$10$y6jt2UCgNttEEyVvfnIHYOOGVAFqcgleYk97HCRIHF3oJpc/T/DDW','salle'),
    ('Moon','moon@gmail.com','$2a$10$wDvPzfrpYeqpdEP.sos9UuW3dzB5lD8MII6iYjvCsyzLPPjkdwJKq','salle'),
    ('Deimos','deimos@gmail.com','$2a$10$nI5BrzPrcZf4zi5WEyqI4.8GrlCiHJXsYQL2OaWbpuy7GHnjmKEBm','security'),
    ('Europa','europa@gmail.com','$2a$10$3KrmGHDqxfPS3hthFOpZO.mkED31AL5bYmMRyvaC0MHyPwQIiWHK6','security');
