INSERT INTO customer_comp (customerid, name, email, objectname, modulesequence) VALUES 
  ('1119ff33-6dd2-4c0a-b113-a90ee32a01ca','Andi B.','andi@user.com','webshop.customer.core.CustomerImpl','customer_impl'),
  ('1129ff33-6dd2-4c0a-b113-a90ee32a01ca','Budi C.','budi@user.com','webshop.customer.core.CustomerImpl','customer_impl'),
  ('1139ff33-6dd2-4c0a-b113-a90ee32a01ca','Cahyo D.','cahyo@user.com','webshop.customer.core.CustomerImpl','customer_impl'),
  ('1149ff33-6dd2-4c0a-b113-a90ee32a01ca','Dewi E.','dewi@user.com','webshop.customer.core.CustomerImpl','customer_impl'),
  ('1159ff33-6dd2-4c0a-b113-a90ee32a01ca','Eka F.','eka@user.com','webshop.customer.core.CustomerImpl','customer_impl'),
  ('1169ff33-6dd2-4c0a-b113-a90ee32a01ca','Fajar G.','fajar@user.com','webshop.customer.core.CustomerImpl','customer_impl')
ON CONFLICT (customerid) DO NOTHING;

INSERT INTO customer_impl (customerid) VALUES 
  ('1119ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
  ('1129ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
  ('1139ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
  ('1149ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
  ('1159ff33-6dd2-4c0a-b113-a90ee32a01ca'), 
  ('1169ff33-6dd2-4c0a-b113-a90ee32a01ca')
ON CONFLICT (customerid) DO NOTHING;
