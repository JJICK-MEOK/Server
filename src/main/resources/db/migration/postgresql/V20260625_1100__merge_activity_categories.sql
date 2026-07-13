UPDATE activities
SET category = 'CULTURE'
WHERE category IN ('MUSIC', 'DANCE', 'ETC');

UPDATE activities
SET category = 'CAREER'
WHERE category = 'SELF_DEVELOPMENT';
