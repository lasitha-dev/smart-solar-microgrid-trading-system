/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: RESTful API Controller managing microgrid station nodes, scheduling, proximity discovery, and deactivation guards.
 * Author: Member 2
 */

using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Controllers;

/// <summary>
/// Description: Provides RESTful endpoints for microgrid station registration, updating, proximity discovery,
/// and deactivation with FAT service reservation validation.
/// Author: Member 2
/// </summary>
[ApiController]
[Route("api/[controller]")]
[Produces("application/json")]
public class StationsController : ControllerBase
{
    private readonly IStationService _stationService;

    /// <summary>
    /// Initializes a new instance of the <see cref="StationsController"/> class.
    /// </summary>
    /// <param name="stationService">The station business logic service.</param>
    /// <exception cref="ArgumentNullException">Thrown when dependencies are null.</exception>
    public StationsController(IStationService stationService)
    {
        _stationService = stationService ?? throw new ArgumentNullException(nameof(stationService));
    }

    /// <summary>
    /// Retrieves all solar microgrid stations.
    /// </summary>
    /// <returns>HTTP 200 with list of stations.</returns>
    [HttpGet]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ApiResponseDto<List<StationResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAll()
    {
        var stations = await _stationService.GetAllStationsAsync();
        return Ok(ApiResponseDto<List<StationResponseDto>>.Ok(stations, $"Retrieved {stations.Count} station nodes."));
    }

    /// <summary>
    /// Retrieves a single microgrid station by its unique identifier.
    /// </summary>
    /// <param name="id">The unique station identifier.</param>
    /// <returns>HTTP 200 with station details; HTTP 404 if not found.</returns>
    [HttpGet("{id}")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ApiResponseDto<StationResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(string id)
    {
        var station = await _stationService.GetStationByIdAsync(id);
        if (station == null)
        {
            return NotFound(ApiResponseDto<object>.Fail($"Station with ID '{id}' was not found."));
        }

        return Ok(ApiResponseDto<StationResponseDto>.Ok(station));
    }

    /// <summary>
    /// Queries nearby microgrid stations within a given radius using the Haversine formula.
    /// Used by the Android mobile client for map plotting.
    /// </summary>
    /// <param name="lat">Geographic latitude of reference point.</param>
    /// <param name="lng">Geographic longitude of reference point.</param>
    /// <param name="radiusKm">Maximum radial distance in kilometers (defaults to 10 km).</param>
    /// <returns>HTTP 200 with list of nearby stations ordered by proximity.</returns>
    [HttpGet("nearby")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ApiResponseDto<List<NearbyStationDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetNearby(
        [FromQuery] double lat,
        [FromQuery] double lng,
        [FromQuery] double radiusKm = 10.0)
    {
        var nearbyStations = await _stationService.GetNearbyStationsAsync(lat, lng, radiusKm);
        return Ok(ApiResponseDto<List<NearbyStationDto>>.Ok(nearbyStations, $"Found {nearbyStations.Count} stations within {radiusKm} km."));
    }

    /// <summary>
    /// Registers a new solar microgrid station node.
    /// Restricted to Backoffice administrators.
    /// </summary>
    /// <param name="dto">The creation payload.</param>
    /// <returns>HTTP 201 with created station details; HTTP 400 on validation failure.</returns>
    [HttpPost]
    [Authorize(Roles = "Backoffice,Administrator")]
    [ProducesResponseType(typeof(ApiResponseDto<StationResponseDto>), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> Create([FromBody] StationCreateDto dto)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var created = await _stationService.CreateStationAsync(dto);
        return CreatedAtAction(nameof(GetById), new { id = created.Id }, ApiResponseDto<StationResponseDto>.Ok(created, "Station registered successfully."));
    }

    /// <summary>
    /// Updates an existing microgrid station node with partial update support.
    /// Restricted to Backoffice administrators.
    /// Enforces reservation-guarded deactivation if status is changed to Inactive.
    /// </summary>
    /// <param name="id">The unique station identifier.</param>
    /// <param name="dto">The update payload.</param>
    /// <returns>HTTP 200 with updated details; HTTP 404 if not found; HTTP 409 if deactivation is blocked.</returns>
    [HttpPut("{id}")]
    [Authorize(Roles = "Backoffice,Administrator")]
    [ProducesResponseType(typeof(ApiResponseDto<StationResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> Update(string id, [FromBody] StationUpdateDto dto)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        try
        {
            var updated = await _stationService.UpdateStationAsync(id, dto);
            if (updated == null)
            {
                return NotFound(ApiResponseDto<object>.Fail($"Station with ID '{id}' was not found."));
            }

            return Ok(ApiResponseDto<StationResponseDto>.Ok(updated, "Station updated successfully."));
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new
            {
                success = false,
                message = ex.Message,
                code = "DEACTIVATION_BLOCKED"
            });
        }
    }

    /// <summary>
    /// Soft-deletes / deactivates a microgrid station node.
    /// Restricted to Backoffice administrators.
    /// Enforces the FAT Service rule: Station cannot be deactivated if it has any Pending or Approved reservations.
    /// </summary>
    /// <param name="id">The unique identifier of the station to deactivate.</param>
    /// <returns>HTTP 200 on success; HTTP 404 if not found; HTTP 409 Conflict if deactivation is blocked by active reservations.</returns>
    [HttpDelete("{id}")]
    [Authorize(Roles = "Backoffice,Administrator")]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> Deactivate(string id)
    {
        try
        {
            var success = await _stationService.DeactivateStationAsync(id);
            if (!success)
            {
                return NotFound(ApiResponseDto<object>.Fail($"Station with ID '{id}' was not found."));
            }

            return Ok(ApiResponseDto<object>.Ok(null, "Station deactivated successfully."));
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new
            {
                success = false,
                message = ex.Message,
                code = "DEACTIVATION_BLOCKED"
            });
        }
    }
}
