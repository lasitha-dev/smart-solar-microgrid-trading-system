/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Generic wrapper DTO for standardized API response payloads and messages.
 */

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Provides a consistent JSON envelope structure for API responses across all endpoints.
/// </summary>
/// <typeparam name="T">The type of the encapsulated payload data.</typeparam>
public class ApiResponseDto<T>
{
    /// <summary>
    /// Gets or sets a value indicating whether the requested operation was successful.
    /// </summary>
    public bool Success { get; set; }

    /// <summary>
    /// Gets or sets a human-readable informational or error message.
    /// </summary>
    public string Message { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the optional payload data returned by the operation.
    /// </summary>
    public T? Data { get; set; }

    /// <summary>
    /// Factory helper to build a successful response wrapper.
    /// </summary>
    /// <param name="data">The payload data.</param>
    /// <param name="message">An optional success message.</param>
    /// <returns>A new <see cref="ApiResponseDto{T}"/> marked as successful.</returns>
    public static ApiResponseDto<T> Ok(T? data, string message = "Operation completed successfully.") =>
        new() { Success = true, Message = message, Data = data };

    /// <summary>
    /// Factory helper to build an error response wrapper.
    /// </summary>
    /// <param name="message">The failure reason.</param>
    /// <returns>A new <see cref="ApiResponseDto{T}"/> marked as failed.</returns>
    public static ApiResponseDto<T> Fail(string message) =>
        new() { Success = false, Message = message, Data = default };
}
