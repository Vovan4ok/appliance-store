-- Orders state: approved=true (approved by employee), approved=null (submitted for review), approved=false (in cart)
-- Clients: 1=Mercury, 2=Venus, 3=Earth, 4=Mars, 5=Jupiter, 6=Saturn, 7=Uranus, 8=Neptune
-- Employees: 1=Phobos, 2=Moon, 3=Deimos, 4=Europa

-- ORDER 1: Mercury — approved by Phobos
INSERT INTO orders (approved, client_id, employee_id) VALUES (true, 1, 1);
-- ORDER 2: Venus — approved by Moon
INSERT INTO orders (approved, client_id, employee_id) VALUES (true, 2, 2);
-- ORDER 3: Jupiter — approved by Phobos
INSERT INTO orders (approved, client_id, employee_id) VALUES (true, 5, 1);
-- ORDER 4: Earth — submitted, awaiting review
INSERT INTO orders (approved, client_id, employee_id) VALUES (null, 3, null);
-- ORDER 5: Mars — submitted, awaiting review
INSERT INTO orders (approved, client_id, employee_id) VALUES (null, 4, null);
-- ORDER 6: Neptune — submitted, awaiting review
INSERT INTO orders (approved, client_id, employee_id) VALUES (null, 8, null);
-- ORDER 7: Saturn — active cart
INSERT INTO orders (approved, client_id, employee_id) VALUES (false, 6, null);
-- ORDER 8: Uranus — active cart
INSERT INTO orders (approved, client_id, employee_id) VALUES (false, 7, null);

-- ORDER ROWS
-- Order 1 (Mercury, approved): home setup — fridge + washing machine + dishwasher
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (1, 8,  1299.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (1, 18,  699.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (1, 27,  649.99, 1);

-- Order 2 (Venus, approved): living room — OLED TV + espresso machine + cordless vacuum
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (2, 46, 1299.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (2, 64,  399.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (2, 77,  699.99, 1);

-- Order 3 (Jupiter, approved): kitchen + comfort — oven + split AC + kettle x2 + toaster
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (3, 35,  799.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (3, 40,  799.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (3, 50,   59.99, 2);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (3, 58,   34.99, 1);

-- Order 4 (Earth, submitted): LG washer + Miele dishwasher + built-in oven
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (4, 19,  749.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (4, 29, 1399.00, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (4, 38,  899.00, 1);

-- Order 5 (Mars, submitted): Sony TV + bean-to-cup coffee + blender + food processor
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (5, 47,  849.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (5, 65,  499.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (5, 71,  129.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (5, 74,  199.99, 1);

-- Order 6 (Neptune, submitted): French door fridge + robot vacuum + steam iron
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (6, 9,  1099.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (6, 78,  349.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (6, 83,   89.99, 1);

-- Order 7 (Saturn, in cart): Dyson cordless vacuum + Miele canister vacuum
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (7, 77, 699.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (7, 80, 499.99, 1);

-- Order 8 (Uranus, in cart): Siemens espresso + Dyson kettle + retro toaster + coffee pods x3
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (8, 68, 279.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (8, 52, 129.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (8, 61,  39.99, 1);
INSERT INTO order_row (order_id, appliance_id, amount, number) VALUES (8, 66,  89.99, 3);
