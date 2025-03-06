INSERT INTO order_comp (date, zipcode, country, amount, quantity, city, street, orderid, catalog_catalogid, state, customer_customerid, status, objectname, modulesequence)

VALUES
    -- Bogor
    ('2024-10-24 09:38:42', 16154, 'Indonesia', 200, FLOOR(RANDOM() * 5 + 1), 'Bogor', 'Jl. Juanda no.16', '1009ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 0), 'Jawa Barat', '1119ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    ('2024-10-24 09:38:42', 16154, 'Indonesia', 200, FLOOR(RANDOM() * 5 + 1), 'Bogor', 'Jl. Pajajaran', '1019ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 1), 'Jawa Barat', '1119ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    ('2024-10-24 09:38:42', 16154, 'Indonesia', 200, FLOOR(RANDOM() * 5 + 1), 'Bogor', 'Jl. Bangbarung', '1029ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 2), 'Jawa Barat', '1119ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    
    -- Balikpapan
    ('2024-10-24 09:38:42', 76123, 'Indonesia', 250, FLOOR(RANDOM() * 5 + 1), 'Balikpapan', 'Jl. MT Haryono', '1039ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 0), 'Kalimantan Timur', '1129ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    ('2024-10-24 09:38:42', 76123, 'Indonesia', 250, FLOOR(RANDOM() * 5 + 1), 'Balikpapan', 'Jl. Soekarno Hatta', '1049ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 2), 'Kalimantan Timur', '1129ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    ('2024-10-24 09:38:42', 76123, 'Indonesia', 250, FLOOR(RANDOM() * 5 + 1), 'Balikpapan', 'Jl. Jend. Sudirman', '1059ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 3), 'Kalimantan Timur', '1129ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    
	-- Aceh
    ('2024-10-24 09:38:42', 23123, 'Indonesia', 300, FLOOR(RANDOM() * 5 + 1), 'Aceh', 'Jl. Ahmad Yani', '1069ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 0), 'Aceh', '1139ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    ('2024-10-24 09:38:42', 23123, 'Indonesia', 300, FLOOR(RANDOM() * 5 + 1), 'Aceh', 'Jl. Teuku Umar', '1079ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 1), 'Aceh', '1139ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl'),
    ('2024-10-24 09:38:42', 23123, 'Indonesia', 300, FLOOR(RANDOM() * 5 + 1), 'Aceh', 'Jl. Panglima Nyak Makam', '1089ff33-6dd2-4c0a-b113-a90ee32a01ca', (SELECT catalogid FROM catalog_impl LIMIT 1 OFFSET 2), 'Aceh', '1139ff33-6dd2-4c0a-b113-a90ee32a01ca', 'Not Paid', 'webshop.order.core.OrderImpl', 'order_impl');

INSERT INTO order_impl (orderid)
SELECT orderid FROM order_comp;
