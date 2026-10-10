ALTER TABLE product_promotion DROP CONSTRAINT product_promotion_pkey;
ALTER TABLE product_promotion DROP COLUMN id;
ALTER TABLE product_promotion ADD PRIMARY KEY(product_id,promotion_id);
