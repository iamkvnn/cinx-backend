DROP TABLE IF EXISTS user_device_token;
ALTER TABLE user
  DROP COLUMN role,
  DROP COLUMN status,
  DROP COLUMN xp;
