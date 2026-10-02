-- Per-account override of the minimum gap between appointments; NULL falls back to remo.appointments.buffer-minutes
ALTER TABLE users
    ADD COLUMN appointment_buffer_minutes INTEGER CHECK (appointment_buffer_minutes BETWEEN 0 AND 1440);
