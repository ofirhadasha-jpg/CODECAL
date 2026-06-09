# VMM – Vending Machine Manager | Architecture

## System Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)              │
│  Dashboard | Products | Machines | Transactions | Customers     │
├─────────────────────────────────────────────────────────────────┤
│                   Business Logic / Services                     │
│  PricingService | WhatsAppProductService | NotificationService  │
├─────────────────────────────────────────────────────────────────┤
│                    Repository Layer                             │
│  ProductRepository | MachineRepository | TransactionRepository  │
├─────────────────────────────────────────────────────────────────┤
│              Data Layer (Room DB + Retrofit)                    │
│  Entities: Product, Machine, MachineSlot, Transaction, Customer │
│  APIs: PricezApi, SnaplistApi                                   │
├─────────────────────────────────────────────────────────────────┤
│                   VMC SDK Layer (RS232)                         │
│  SerialPortManager → VMCFrame → VMCManager                     │
│  Commands: Payment(0x21-0x28) | Inventory(0x11-0x17)           │
│            Dispense(0x01-0x06) | Status(0x30-0x41)             │
└─────────────────────────────────────────────────────────────────┘
```

## VMC Protocol Flow

```
Upper Computer          VMC
      │                  │
      │──── POLL ───────>│  (every 200ms)
      │<─── ACK ─────────│
      │                  │
      │──── [CMD] ──────>│  STX|LEN|ADDR|CMD|DATA...|CHK|ETX
      │<─── ACK ─────────│
      │<─── [RESP] ──────│  STX|LEN|ADDR|RESP|DATA...|CHK|ETX
      │──── ACK ─────────>│
```

## WhatsApp Product Upload Flow

```
1. User photographs product on phone
2. Shares image via WhatsApp → forwards to this app (SEND intent)
   OR sends product details + image via WhatsApp Business API webhook
3. App receives Intent.ACTION_SEND with image URI
4. WhatsAppProductService.processIncomingProduct():
   a. Opens image → scales to 800×560, centers on white canvas
   b. Calls PricingService → fetches Pricez + Snaplist prices
   c. Calculates suggested sale price (max of cost+20% or market+15%)
   d. Creates/updates Product in Room DB
   e. If VMC connected: syncs price + product ID to machine
5. Returns confirmation with product ID + suggested price
```

## Database Schema

```sql
products:     id, name, barcode, costPrice, salePrice, imageUri,
              productionDate, warrantyDate, vmcProductId, stockCount, minStock

machines:     id, serialNumber, name, location, machineType,
              vmcAddress, status, temperature, doorOpen

machine_slots: id, machineId→machines, slotNumber, productId→products,
               quantity, maxQuantity, price, vmcSlotId

transactions: id, machineId, productId, slotId, productName,
              unitPrice, totalAmount, paymentMethod, status, customerId

customers:    id, name, phone, email, fcmToken,
              totalPurchases, totalSpent, smsEnabled, pushEnabled
```

## Key Files

| File | Purpose |
|------|---------|
| `vmc/protocol/VMCProtocol.kt` | All command constants (0x01–0x41) |
| `vmc/protocol/VMCFrame.kt` | Frame encode/decode + XOR checksum |
| `vmc/serial/SerialPortManager.kt` | USB-RS232 read/write (57600 baud) |
| `vmc/VMCManager.kt` | High-level VMC API + POLL loop |
| `service/WhatsAppProductService.kt` | Image processing + product creation |
| `service/PricingService.kt` | Pricez + Snaplist price suggestions |
| `data/repository/TransactionRepository.kt` | Full vend flow (VMC + DB + notify) |
