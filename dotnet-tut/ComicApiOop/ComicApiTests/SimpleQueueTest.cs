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

    [SetUp]
    public void Setup()
    {
        _mockMessageBusLogger = new Mock<ILogger<SimpleMessageBus>>();
    }

    [Test]
    public async Task BatchDequeue_EnqueueMessages_CallbackReceivesMessages()
    {

        ConcurrentBag<string> processedMessages = new ConcurrentBag<string>();
        int processedCount = 0;
        Random random = new Random();

        SimpleMessageBus SimpleMessageBus = new SimpleMessageBus(_mockMessageBusLogger.Object);
        // Arrange
        SimpleMessageBus.RegisterQueue<string>(new SimpleQueue<string>());

        List<String> messagesToEnqueue = new List<string>();
        for (int i = 0; i < 150; i++)
        {
            messagesToEnqueue.Add("message" + i);
        }

        // Create a task completion source to signal when test is complete
        var tcs = new TaskCompletionSource<bool>();

        // Setup callback to process messages
        Task<IValue[]> ProcessBatch(int batchSize, List<string?> messages)
        {
            Console.WriteLine($"Number of dequeued messages: {batchSize}");
            for (int i = 0; i < messages.Count; i++)
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
    public async Task SimpleRequestResponseFlowTest()
    {
        // To add random delay between requests. 
        // So that some requests are batched to max size and some not
        Random random = new Random();
        var messageBus = new SimpleMessageBus(_mockMessageBusLogger.Object);
        messageBus.RegisterQueue<TestRequestObj>(new SimpleQueue<TestRequestObj>());


        Task<IValue[]> ProcessBatch(int batchSize, List<TestRequestObj?> messages)
        {
            Console.WriteLine($"Number of dequeued messages: {batchSize}");
            return Task.Run(() =>
            {
                for (int i = 0; i < messages.Count; i++)
                {
                    if (messages[i] != null)
                    {
                        var request = messages[i]!;
                        var response = new TestResponseObj(request.Id, request.Value * 2);
                        // Set the result on the TaskCompletionSource instead of returning for SimpleMap
                        request.ResponseSrc.TrySetResult(response);
                    }
                }
                return new IValue[0];
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

        // Await all responses using TaskCompletionSource - no polling needed!
        var responseTasks = requests.Select(r => r.ResponseSrc.Task).ToArray();
        var responses = await Task.WhenAll(responseTasks);

        // Assert all responses
        for (int i = 0; i < requests.Count; i++)
        {
            Assert.That(responses[i].Value, Is.EqualTo(requests[i].Value * 2), 
                $"Response value should be double the request value for request {i}");
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
        public TaskCompletionSource<TestResponseObj> ResponseSrc { get; set; }

        public TestRequestObj(int value)
        {
            this.Value = value;
            this.Id = GetHashCode();
            this.ResponseSrc = new TaskCompletionSource<TestResponseObj>();
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

    public class TestResponseObj : IValue
    {
        public int Id { get; }
        public int Value { get; set; }

        public TestResponseObj(int id, int value)
        {
            this.Id = id;
            this.Value = value;
        }
    }
}