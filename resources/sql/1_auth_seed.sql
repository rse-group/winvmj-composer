INSERT INTO auth_user_comp (id) VALUES ('2a0859e2-e73f-4ebe-85c0-6a39d231bbbb') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('b097505f-be60-414b-83a8-cf4f44bc30ed') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1109ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('2109ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1119ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1129ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1139ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1149ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1159ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('1169ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('2119ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('2129ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('2139ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('2149ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;
INSERT INTO auth_user_comp (id) VALUES ('2159ff33-6dd2-4c0a-b113-a90ee32a01ca') ON CONFLICT DO NOTHING;

INSERT INTO auth_user_impl (id, allowedPermissions, name, email) 
VALUES 
    ('2a0859e2-e73f-4ebe-85c0-6a39d231bbbb', '', 'Syifa', 'syifa.afra@gmail.com'),
    ('b097505f-be60-414b-83a8-cf4f44bc30ed', '', 'admin', 'admin@user.com'),
    ('1119ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Andi B.', 'andi@user.com'),
    ('1129ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Budi C.', 'budi@user.com'),
    ('1139ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Cahyo D.', 'cahyo@user.com'),
    ('1149ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Dewi E.', 'dewi@user.com'),
    ('1159ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Eka F.', 'eka@user.com'),
    ('1169ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Fajar G.', 'fajar@user.com'),
    ('2119ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Hendra', 'hendra@user.com'),
    ('2129ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Indah', 'indah@user.com'),
    ('2139ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Joko', 'joko@user.com'),
    ('2149ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Kiki', 'kiki@user.com'),
    ('2159ff33-6dd2-4c0a-b113-a90ee32a01ca', '', 'Lina', 'lina@user.com')
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_passworded (id, password, user_id)
VALUES 
    ('2a0859e2-e73f-4ebe-85c0-6a39d231bbbb', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '2a0859e2-e73f-4ebe-85c0-6a39d231bbbb'),
    ('b097505f-be60-414b-83a8-cf4f44bc30ed', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', 'b097505f-be60-414b-83a8-cf4f44bc30ed'),
    ('1119ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '1119ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('1129ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '1129ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('1139ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '1139ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('1149ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '1149ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('1159ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '1159ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('1169ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '1169ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('2119ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '2119ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('2129ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '2129ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('2139ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '2139ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('2149ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '2149ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('2159ff33-6dd2-4c0a-b113-a90ee32a01ca', '349cbccafc082902f6d88098da92b998129d98c079996b96f305705ffddc67baa935e07353a00b6068e6b0f8e1245ee8d499c80ece5232ad938825cb292bce3b', '2159ff33-6dd2-4c0a-b113-a90ee32a01ca')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_comp (id) VALUES 
    ('75f6727e-66f8-484f-b77f-83eeec82cd10'),
    ('12372338-2822-420d-8c06-cff0d411d776'),
    ('12472338-2822-420d-8c06-cff0d411d776')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_impl (id,name,allowedPermissions) VALUES 
	('75f6727e-66f8-484f-b77f-83eeec82cd10','Administrator','administrator'),
	('12372338-2822-420d-8c06-cff0d411d776','Customer','CreateOrder,HistoryOrder,CancelOrder'),
	('12472338-2822-420d-8c06-cff0d411d776','Seller','CreateCatalog,UpdateCatalog,DeleteCatalog') 
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_role_comp (id)
VALUES
    ('01eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('02eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('03eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('04eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('05eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('06eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('07eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('08eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('09eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('10eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('11eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('12eea95e-549a-4148-b7cf-27748b9cacfb'),
    ('13eea95e-549a-4148-b7cf-27748b9cacfb')
    
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_role_impl (id, authRole, authUser)
VALUES
    ('01eea95e-549a-4148-b7cf-27748b9cacfb', '75f6727e-66f8-484f-b77f-83eeec82cd10', '2a0859e2-e73f-4ebe-85c0-6a39d231bbbb'),
    ('02eea95e-549a-4148-b7cf-27748b9cacfb', '75f6727e-66f8-484f-b77f-83eeec82cd10', 'b097505f-be60-414b-83a8-cf4f44bc30ed'),
    ('03eea95e-549a-4148-b7cf-27748b9cacfb', '12372338-2822-420d-8c06-cff0d411d776', '1119ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('04eea95e-549a-4148-b7cf-27748b9cacfb', '12372338-2822-420d-8c06-cff0d411d776', '1129ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('05eea95e-549a-4148-b7cf-27748b9cacfb', '12372338-2822-420d-8c06-cff0d411d776', '1139ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('06eea95e-549a-4148-b7cf-27748b9cacfb', '12372338-2822-420d-8c06-cff0d411d776', '1149ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('07eea95e-549a-4148-b7cf-27748b9cacfb', '12372338-2822-420d-8c06-cff0d411d776', '1159ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('08eea95e-549a-4148-b7cf-27748b9cacfb', '12372338-2822-420d-8c06-cff0d411d776', '1169ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('09eea95e-549a-4148-b7cf-27748b9cacfb', '12472338-2822-420d-8c06-cff0d411d776', '2119ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('10eea95e-549a-4148-b7cf-27748b9cacfb', '12472338-2822-420d-8c06-cff0d411d776', '2129ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('11eea95e-549a-4148-b7cf-27748b9cacfb', '12472338-2822-420d-8c06-cff0d411d776', '2139ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('12eea95e-549a-4148-b7cf-27748b9cacfb', '12472338-2822-420d-8c06-cff0d411d776', '2149ff33-6dd2-4c0a-b113-a90ee32a01ca'),
    ('13eea95e-549a-4148-b7cf-27748b9cacfb', '12472338-2822-420d-8c06-cff0d411d776', '2159ff33-6dd2-4c0a-b113-a90ee32a01ca') 
ON CONFLICT DO NOTHING;