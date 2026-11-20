-- The owl's fifth slot, HOLD (something held in its wings), and eight more wardrobe items; their art ships in the web
-- app's Rive file. Trung thu 2027 (rằm tháng 8 = 15 Sep 2027) runs a week either side, as Tết does in V24.

ALTER TABLE owl_wardrobe ADD COLUMN hold VARCHAR(40);

INSERT INTO owl_skin (id, version, created_at, updated_at, code, slot, unlock_rule, streak_days, season_starts_on, season_ends_on, name, sort_order, active) VALUES
  (UUID(), 0, NOW(6), NOW(6), 'gold',         'PALETTE', 'FREE',   NULL, NULL,         NULL,         'Cú vàng nắng',     8, 1),
  (UUID(), 0, NOW(6), NOW(6), 'headphones',   'HEAD',    'FREE',   NULL, NULL,         NULL,         'Tai nghe',         9, 1),
  (UUID(), 0, NOW(6), NOW(6), 'book',         'HOLD',    'FREE',   NULL, NULL,         NULL,         'Sách',            10, 1),
  (UUID(), 0, NOW(6), NOW(6), 'ear-pencil',   'HEAD',    'STREAK', 3,    NULL,         NULL,         'Bút chì cài tai', 11, 1),
  (UUID(), 0, NOW(6), NOW(6), 'cinnamon',     'PALETTE', 'STREAK', 7,    NULL,         NULL,         'Cú quế',          12, 1),
  (UUID(), 0, NOW(6), NOW(6), 'helmet',       'HEAD',    'STREAK', 7,    NULL,         NULL,         'Nón bảo hiểm',    13, 1),
  (UUID(), 0, NOW(6), NOW(6), 'peach',        'PALETTE', 'STREAK', 30,   NULL,         NULL,         'Cú hồng đào',     14, 1),
  (UUID(), 0, NOW(6), NOW(6), 'star-lantern', 'HOLD',    'SEASON', NULL, '2027-09-08', '2027-09-22', 'Đèn ông sao',     15, 1);
