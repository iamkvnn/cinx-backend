-- The tutor owl's wardrobe: the skin catalog (seeded here, its art ships in the web app's Rive file; admins switch
-- items on or off and date the seasons), what each learner's owl wears, and the season skins learners earned.
-- Streak skins are derived from user_streak.highest_streak and never stored.

CREATE TABLE IF NOT EXISTS owl_skin (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  code VARCHAR(40) NOT NULL,
  slot VARCHAR(20) NOT NULL,
  unlock_rule VARCHAR(20) NOT NULL,
  streak_days INT,
  season_starts_on DATE,
  season_ends_on DATE,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL,
  active BIT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owl_skin_code (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS owl_wardrobe (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  created_by VARCHAR(100),
  updated_by VARCHAR(100),
  user_id VARCHAR(255) NOT NULL,
  palette VARCHAR(40),
  head VARCHAR(40),
  face VARCHAR(40),
  effect VARCHAR(40),
  PRIMARY KEY (id),
  UNIQUE KEY uk_owl_wardrobe_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS owl_skin_grant (
  id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  skin_code VARCHAR(40) NOT NULL,
  granted_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owl_skin_grant_user_skin (user_id, skin_code)
) ENGINE=InnoDB;

-- the first wardrobe; Tết 2027 (mùng 1 = 6 Feb 2027) runs a week either side
INSERT INTO owl_skin (id, version, created_at, updated_at, code, slot, unlock_rule, streak_days, season_starts_on, season_ends_on, name, sort_order, active) VALUES
  (UUID(), 0, NOW(6), NOW(6), 'brown',         'PALETTE', 'FREE',   NULL, NULL,         NULL,         'Cú nâu',          0, 1),
  (UUID(), 0, NOW(6), NOW(6), 'grad-cap',      'HEAD',    'FREE',   NULL, NULL,         NULL,         'Mũ tốt nghiệp',   1, 1),
  (UUID(), 0, NOW(6), NOW(6), 'round-glasses', 'FACE',    'STREAK', 3,    NULL,         NULL,         'Kính tròn',       2, 1),
  (UUID(), 0, NOW(6), NOW(6), 'snow',          'PALETTE', 'STREAK', 7,    NULL,         NULL,         'Cú tuyết',        3, 1),
  (UUID(), 0, NOW(6), NOW(6), 'sparkle-trail', 'EFFECT',  'STREAK', 30,   NULL,         NULL,         'Vệt lấp lánh',    4, 1),
  (UUID(), 0, NOW(6), NOW(6), 'crown',         'HEAD',    'STREAK', 100,  NULL,         NULL,         'Vương miện',      5, 1),
  (UUID(), 0, NOW(6), NOW(6), 'tet-scarf',     'PALETTE', 'SEASON', NULL, '2027-01-30', '2027-02-14', 'Khăn Tết đỏ',     6, 1),
  (UUID(), 0, NOW(6), NOW(6), 'non-la',        'HEAD',    'SEASON', NULL, '2027-01-30', '2027-02-14', 'Nón lá',          7, 1);
