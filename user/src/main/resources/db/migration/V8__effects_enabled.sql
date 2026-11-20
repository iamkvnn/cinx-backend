-- The learner's "Hiệu ứng trang trí" switch (profile tab Hiển thị): whether the owl and celebration animations play; on by default.
ALTER TABLE `user` ADD COLUMN effects_enabled BIT(1) NOT NULL DEFAULT 1;
