ALTER TABLE client   ADD COLUMN phone        VARCHAR(20);
ALTER TABLE client   ADD COLUMN date_of_birth DATE;
ALTER TABLE client   ADD COLUMN avatar_path   VARCHAR(255);

ALTER TABLE employee ADD COLUMN phone        VARCHAR(20);
ALTER TABLE employee ADD COLUMN date_of_birth DATE;
ALTER TABLE employee ADD COLUMN avatar_path   VARCHAR(255);