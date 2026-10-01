INSERT INTO categories (name, description)
SELECT 'Arts & Culture', 'Arts & Culture'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Arts & Culture');

INSERT INTO categories (name, description)
SELECT 'Food & Cooking', 'Food & Cooking'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Food & Cooking');

INSERT INTO categories (name, description)
SELECT 'Lifestyle', 'Lifestyle'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Lifestyle');

INSERT INTO categories (name, description)
SELECT 'Music', 'Music'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Music');

INSERT INTO categories (name, description)
SELECT 'Travel & Places', 'Travel & Places'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Travel & Places');
