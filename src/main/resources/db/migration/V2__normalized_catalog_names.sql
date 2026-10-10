CREATE UNIQUE INDEX uq_brand_lower_name ON brand(lower(name));
CREATE UNIQUE INDEX uq_category_lower_name ON category(lower(name));
