# Simple DOD Framework - Documentation Index

## 📚 Documentation Guide

Choose the document that best fits your needs:

### 🚀 Getting Started

**[QUICK_START.md](./QUICK_START.md)** - **Start here!**
- Quick code examples
- How to use in endpoints and services
- How to add your message processor
- 5-minute setup guide

### 📖 Complete Guides

**[README.md](./README.md)** - Framework Overview
- What is the Simple DOD Framework?
- Core concepts and architecture
- Detailed usage instructions
- Benefits and use cases
- Performance considerations

**[DEPENDENCY_INJECTION_SETUP.md](./DEPENDENCY_INJECTION_SETUP.md)** - DI Configuration
- How DI is configured
- Service registration details
- Request and processing flow
- Usage patterns in your code
- Monitoring and tuning

### 🏗️ Architecture

**[ARCHITECTURE.md](./ARCHITECTURE.md)** - System Design
- Detailed architecture diagrams
- Component interactions
- Data flow visualization
- Thread safety guarantees
- Performance characteristics
- Scaling considerations

### ✅ Project Status

**[SUMMARY.md](./SUMMARY.md)** - Completion Summary
- What was implemented
- Verification steps
- Files created/modified
- Current status
- Next steps

---

## 📁 Framework Files

### Core Components
- `SimpleQueue.cs` - Thread-safe message queue
- `SimpleMap.cs` - Thread-safe response storage
- `SimpleMessageBus.cs` - Message routing coordinator

### DI Integration
- `SimpleMapService.cs` - DI wrapper for SimpleMap
- `SimpleMessageBusService.cs` - DI wrapper for SimpleMessageBus
- `../Services/MessageProcessingHostedService.cs` - Background processor

### Models
- `../Models/RequestResponse.cs` - Example request/response models

---

## 🎯 Quick Links by Task

### "I want to use this in my code"
→ Read [QUICK_START.md](./QUICK_START.md)

### "I want to understand how it works"
→ Read [ARCHITECTURE.md](./ARCHITECTURE.md)

### "I want to set up my own processor"
→ Read [QUICK_START.md](./QUICK_START.md) Section 3

### "I want to see the complete setup"
→ Read [DEPENDENCY_INJECTION_SETUP.md](./DEPENDENCY_INJECTION_SETUP.md)

### "I want to know what was done"
→ Read [SUMMARY.md](./SUMMARY.md)

### "I want detailed framework docs"
→ Read [README.md](./README.md)

---

## 💡 Common Scenarios

### Scenario 1: Add a New Message Type
1. Define request and response models ([QUICK_START.md](./QUICK_START.md))
2. Register queue in `MessageProcessingHostedService.cs`
3. Implement batch processor
4. Use in endpoints by injecting services

### Scenario 2: Use in an Endpoint
```csharp
// See QUICK_START.md for complete example
app.MapGet("/endpoint", async (
    SimpleMessageBusService messageBus,
    SimpleMapService mapService) => { ... });
```

### Scenario 3: Use in a Service
```csharp
// See QUICK_START.md for complete example
public class MyService
{
    public MyService(
        SimpleMessageBusService messageBus,
        SimpleMapService mapService) { ... }
}
```

---

## 🧪 Testing

Tests are located in: `../../ComicApiTests/SimpleQueueTest.cs`

Run tests:
```bash
cd ../../ComicApiTests
dotnet test
```

---

## 🔍 Troubleshooting

### Issue: Services not available
**Solution**: Check `ProgramDod.cs` for service registration

### Issue: Messages not processed
**Solution**: Check `MessageProcessingHostedService.cs` for queue registration

### Issue: Response not found
**Solution**: Check timeout and processor implementation

### Issue: Memory growing
**Solution**: Make sure to call `mapService.Remove()` after retrieving responses

---

## 📊 Documentation Map

```
INDEX.md (You are here)
    │
    ├─ QUICK_START.md ────────────→ Start here!
    │     │
    │     ├─ How to use in endpoints
    │     ├─ How to use in services
    │     └─ How to add processors
    │
    ├─ README.md ─────────────────→ Framework overview
    │     │
    │     ├─ Core concepts
    │     ├─ Usage guide
    │     └─ Performance tips
    │
    ├─ DEPENDENCY_INJECTION_SETUP.md → DI details
    │     │
    │     ├─ Service registration
    │     ├─ Request flow
    │     └─ Usage patterns
    │
    ├─ ARCHITECTURE.md ───────────→ System design
    │     │
    │     ├─ Architecture diagrams
    │     ├─ Data flow
    │     └─ Thread safety
    │
    └─ SUMMARY.md ────────────────→ Project status
          │
          ├─ What was done
          ├─ Verification
          └─ Next steps
```

---

## 🎓 Learning Path

### Beginner
1. Read [QUICK_START.md](./QUICK_START.md)
2. Look at example in `ProgramDod.cs`
3. Try using services in an endpoint

### Intermediate
1. Read [README.md](./README.md)
2. Read [DEPENDENCY_INJECTION_SETUP.md](./DEPENDENCY_INJECTION_SETUP.md)
3. Implement your own processor

### Advanced
1. Read [ARCHITECTURE.md](./ARCHITECTURE.md)
2. Study thread safety guarantees
3. Optimize batch size and polling

---

## 📝 Notes

- All components are **thread-safe**
- Services are **singletons** (shared across all requests)
- Background processing is **automatic** (starts with app)
- Documentation is **comprehensive** (5 detailed guides)
- Example code is **included** (see `ProgramDod.cs`)

---

## ✅ Status

**Ready for Production Use**

All components are:
- ✅ Implemented
- ✅ Tested
- ✅ Documented
- ✅ Integrated with DI
- ✅ Verified working

---

## 📞 Need Help?

1. Check [QUICK_START.md](./QUICK_START.md) for common tasks
2. Check [ARCHITECTURE.md](./ARCHITECTURE.md) for design questions
3. Check example endpoint in `../ProgramDod.cs`
4. Check tests in `../../ComicApiTests/SimpleQueueTest.cs`

---

**Last Updated**: October 17, 2025
**Version**: 1.0
**Status**: Complete ✅


