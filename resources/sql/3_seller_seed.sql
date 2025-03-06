INSERT INTO seller_comp (sellerid, name, email, objectname, modulesequence) VALUES 
	('2119ff33-6dd2-4c0a-b113-a90ee32a01ca','Hendra I.','hendra@user.com','webshop.seller.core.SellerImpl','seller_impl'), 
	('2129ff33-6dd2-4c0a-b113-a90ee32a01ca','Indah J.','indah@user.com','webshop.seller.core.SellerImpl','seller_impl'), 
	('2139ff33-6dd2-4c0a-b113-a90ee32a01ca','Joko K.','joko@user.com','webshop.seller.core.SellerImpl','seller_impl'), 
	('2149ff33-6dd2-4c0a-b113-a90ee32a01ca','Kiki L.','kiki@user.com','webshop.seller.core.SellerImpl','seller_impl'), 
	('2159ff33-6dd2-4c0a-b113-a90ee32a01ca','Lina M.','lina@user.com','webshop.seller.core.SellerImpl','seller_impl')
ON CONFLICT DO NOTHING;

INSERT INTO seller_impl (sellerid) VALUES 
	('2119ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
	('2129ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
	('2139ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
	('2149ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
	('2159ff33-6dd2-4c0a-b113-a90ee32a01ca')
ON CONFLICT DO NOTHING;

ALTER TABLE catalog_comp ALTER COLUMN pictureurl TYPE TEXT;
