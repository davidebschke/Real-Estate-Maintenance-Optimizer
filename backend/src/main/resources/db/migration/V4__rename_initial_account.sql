-- Renames the productive account created in V2 to the neutral default name; skipped when its username was already changed in the profile settings
UPDATE users
SET username = 'account_default', display_name = 'Account_Default'
WHERE id = '00000000-0000-0000-0000-000000000001' AND username = 'debschke';
