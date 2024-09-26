CREATE PROCEDURE get_total_accounts_by_city_and_type_and_currency(
    IN city VARCHAR(30),
    IN type VARCHAR(10),
    IN currency VARCHAR(10),
    OUT count_of_accounts INT)
BEGIN
    SELECT COUNT(*) INTO count_of_accounts
    FROM accounts a
    INNER JOIN branches b ON a.branch_name = b.name
    WHERE a.closed_at IS NULL
          AND b.city = city
          AND a.type = type
          AND a.currency = currency;
END