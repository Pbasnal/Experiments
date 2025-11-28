using System.Collections.Concurrent;
using NUnit.Framework;
using ComicApiDod.SimpleQueue;
using Microsoft.Extensions.Logging;
using Moq;

namespace ComicApiTests;

[TestFixture]
public class SimpleQueueTests
{
    private Mock<ILogger<SimpleMessageBus>> _mockMessageBusLogger;
    private Mock<ILogger<SimpleMap>> _mockMapLogger;

    [SetUp]
    public void Setup()
    {
        _mockMessageBusLogger = new Mock<ILogger<SimpleMessageBus>>();
        _mockMapLogger = new Mock<ILogger<SimpleMap>>();
    }

    [Test]
    public async Task BatchDequeue_EnqueueMessages_CallbackReceivesMessages()
    {

        ConcurrentBag<string> processedMessages = new ConcurrentBag<string>();
        int processedCount = 0;
        Random random = new Random();

        SimpleMessageBus SimpleMessageBus = new SimpleMessageBus(_mockMessageBusLogger.Object);
        SimpleMap simpleMap = new SimpleMap(_mockMapLogger.Object);
        // Arrange
        SimpleMessageBus.RegisterQueue<string>(new SimpleQueue<string>(simpleMap));

        List<String> messagesToEnqueue = new List<string>();
        for (int i = 0; i < 150; i++)
        {
            messagesToEnqueue.Add("message" + i);
        }

        // Create a task completion source to signal when test is complete
        var tcs = new TaskCompletionSource<bool>();

        // Setup callback to process messages
        Task<IValue[]> ProcessBatch(int batchSize, string?[] messages)
        {
            Console.WriteLine($"Number of dequeued messages: {batchSize}");
            for (int i = 0; i < batchSize; i++)
            {
                if (messages[i] != null)
                {
                    processedMessages.Add(messages[i]!);
                    processedCount++;
                }
            }

            // Signal test completion when all messages are processed
            if (processedCount >= messagesToEnqueue.Count)
            {
                tcs.TrySetResult(true);
            }
            return Task.Run(()=> new IValue[0]);
        }

        // Start batch dequeue in a separate task
        _ = Task.Run(() => SimpleMessageBus.StartBatchListener<string>(10, ProcessBatch));

        // Enqueue messages with a small delay to simulate real-world scenario
        foreach (var message in messagesToEnqueue)
        {
            SimpleMessageBus.Enqueue(message);

            if (random.NextInt64(100) % 10 == 0) await Task.Delay(20);
        }

        // Wait for processing to complete or timeout
        var processingCompleted = await Task.WhenAny(
            tcs.Task,
            Task.Delay(TimeSpan.FromSeconds(5))
        );

        // Assert
        Assert.That(processingCompleted, Is.EqualTo(tcs.Task), "Processing did not complete in time");
        Assert.That(processedCount, Is.EqualTo(messagesToEnqueue.Count), "Not all messages were processed");

        // Verify that all original messages were processed
        foreach (var message in messagesToEnqueue)
        {
            Assert.Contains(message, processedMessages.ToArray(), "Message was not processed");
        }
    }

    [Test]
    public async Task SimpleMapTest()
    {
        SimpleMap simpleMap = new SimpleMap(_mockMapLogger.Object);

        TestObj testObj1 = new TestObj(5);
        TestObj testObj2 = new TestObj(10);
        TestObj testObj3 = new TestObj(15);

        simpleMap.Add(testObj1);
        simpleMap.Add(testObj2);
        simpleMap.Add(testObj3);

        Assert.That(simpleMap.Find(testObj1.Id, out TestObj? testObjR1), Is.EqualTo(true), "Test object not found");
        Assert.That(simpleMap.Find(testObj2.Id, out TestObj? testObjR2), Is.EqualTo(true), "Test object not found");
        Assert.That(simpleMap.Find(testObj3.Id, out TestObj? testObjR3), Is.EqualTo(true), "Test object not found");

        Assert.That(testObjR1, Is.EqualTo(testObj1));
        Assert.That(testObjR2, Is.EqualTo(testObj2));
        Assert.That(testObjR3, Is.EqualTo(testObj3));
    }

    [Test]
    public async Task SimpleRequestResponseFlowTest()
    {
        // To add random delay between requests. 
        // So that some requests are batched to max size and some not
        Random random = new Random();
        SimpleMap simpleMap = new SimpleMap(_mockMapLogger.Object);
        var messageBus = new SimpleMessageBus(_mockMessageBusLogger.Object);
        messageBus.RegisterQueue<TestRequestObj>(new SimpleQueue<TestRequestObj>(simpleMap));


        Task<IValue[]> ProcessBatch(int batchSize, TestRequestObj?[] messages)
        {
            Console.WriteLine($"Number of dequeued messages: {batchSize}");
            return Task.Run(() =>
            {
                IValue[] result = new IValue[batchSize];
                for (int i = 0; i < batchSize; i++)
                {
                    if (messages[i] != null)
                    {
                        result[i] = new TestObj(messages[i]!.Id, messages[i]!.Value * 2);
                    }
                }
                return result;
            });
        }

        // Start the queue listener
        _ = Task.Run(() => messageBus.StartBatchListener<TestRequestObj>(10, ProcessBatch));


        // Send the requests
        List<TestRequestObj> requests = GetRequests(10);
        foreach (var message in requests)
        {
            messageBus.Enqueue(message);
            if (random.NextInt64(100) % 10 == 0) await Task.Delay(20);
        }


        int numberOfResponses = 0;
        while (numberOfResponses < requests.Count)
        {
            foreach (var request in requests)
            {
                if (simpleMap.Find(request.Id, out TestObj? responseObj))
                {
                    Assert.That(responseObj!.Value, Is.EqualTo(request.Value * 2));
                    numberOfResponses++;
                }
            }
        }
    }

    private List<TestRequestObj> GetRequests(int numberOfRequests)
    {
        List<TestRequestObj> messagesToEnqueue = new List<TestRequestObj>();
        for (int i = 0; i < numberOfRequests; i++)
        {
            messagesToEnqueue.Add(new TestRequestObj(i));
        }

        return messagesToEnqueue;
    }

    public class TestRequestObj : IValue
    {
        public int Id { get; }
        public int Value { get; set; }

        public TestRequestObj(int value)
        {
            this.Value = value;
            this.Id = GetHashCode();
        }
    }

    public class TestObj : IValue
    {
        public int Id { get; }
        public int Value { get; set; }

        public TestObj(int value)
        {
            this.Value = value;
            this.Id = GetHashCode();
        }

        public TestObj(int id, int value)
        {
            this.Value = value;
            this.Id = id;
        }
    }
}