/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Mock async cursor implementation for unit testing MongoDB.Driver operations.
 */

using MongoDB.Driver;

namespace SmartSolarMicrogrid.Api.Tests.TestUtilities;

/// <summary>
/// Mock implementation of MongoDB's <see cref="IAsyncCursor{T}"/> for in-memory unit testing.
/// </summary>
/// <typeparam name="T">The entity type.</typeparam>
public class MockAsyncCursor<T> : IAsyncCursor<T>
{
    private readonly IEnumerable<T> _items;
    private bool _moved;

    public MockAsyncCursor(IEnumerable<T> items)
    {
        _items = items ?? Enumerable.Empty<T>();
    }

    public IEnumerable<T> Current => _items;

    public bool MoveNext(CancellationToken cancellationToken = default)
    {
        if (!_moved)
        {
            _moved = true;
            return true;
        }
        return false;
    }

    public Task<bool> MoveNextAsync(CancellationToken cancellationToken = default)
    {
        return Task.FromResult(MoveNext(cancellationToken));
    }

    public void Dispose()
    {
        GC.SuppressFinalize(this);
    }
}
