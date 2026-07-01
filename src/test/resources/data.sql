INSERT INTO brands (id, name, created_at) VALUES (1, 'Marca Teste CI', NOW()) ON CONFLICT (id) DO NOTHING;
