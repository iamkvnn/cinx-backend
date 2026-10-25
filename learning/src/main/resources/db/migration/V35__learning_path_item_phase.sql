-- A path item names the phase it belongs to ("Nền tảng React"), so a path can group lessons of several courses.
ALTER TABLE learning_path_item ADD COLUMN phase VARCHAR(60) NULL;
