echo "Starting Admin Service (port 8082)..."
java -jar /app/admin-service.jar &
ADMIN_PID=$!
echo "Admin Service started with PID: $ADMIN_PID"

echo "Starting Payment Service (port 8086)..."
java -jar /app/payment-service.jar &
PAYMENT_PID=$!
echo "Payment Service started with PID: $PAYMENT_PID"

echo "Starting User Service (port 8081)..."
java -jar /app/user-service.jar &
USER_PID=$!
echo "User Service started with PID: $USER_PID"

echo "All services are running in the background. Waiting for them to finish..."

wait $ADMIN_PID
wait $PAYMENT_PID
wait $USER_PID

echo "All services have terminated. Exiting container."
