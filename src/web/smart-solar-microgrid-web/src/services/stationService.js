/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: HTTP client service communicating with the central C# Web API /api/stations endpoints.
 * Author: Member 2
 */

import api from './api';

/**
 * Service encapsulating REST communication with the backend Stations Controller.
 */
export const stationService = {
  /**
   * Retrieves all registered microgrid station nodes.
   * @returns {Promise<Array>} List of station response objects.
   */
  async getAllStations() {
    const response = await api.get('/stations');
    return response.data || response;
  },

  /**
   * Retrieves a single microgrid station by its unique identifier.
   * @param {string} id - Station unique identifier.
   * @returns {Promise<Object>} Station response object.
   */
  async getStationById(id) {
    const response = await api.get(`/stations/${id}`);
    return response.data || response;
  },

  /**
   * Creates a new solar microgrid station node.
   * @param {Object} data - Station creation payload.
   * @returns {Promise<Object>} Created station details.
   */
  async createStation(data) {
    const response = await api.post('/stations', data);
    return response.data || response;
  },

  /**
   * Updates attributes of an existing station.
   * @param {string} id - Station unique identifier.
   * @param {Object} data - Update payload.
   * @returns {Promise<Object>} Updated station details.
   */
  async updateStation(id, data) {
    const response = await api.put(`/stations/${id}`, data);
    return response.data || response;
  },

  /**
   * Soft-deletes / deactivates a station node, enforcing FAT reservation rules on the server.
   * @param {string} id - Station unique identifier.
   * @returns {Promise<Object>} Operation outcome.
   */
  async deactivateStation(id) {
    const response = await api.delete(`/stations/${id}`);
    return response.data || response;
  },

  /**
   * Queries nearby stations within radiusKm of GPS coordinates.
   * @param {number} lat - Reference latitude.
   * @param {number} lng - Reference longitude.
   * @param {number} radiusKm - Maximum radial distance in kilometers.
   * @returns {Promise<Array>} List of nearby stations sorted by distance.
   */
  async getNearbyStations(lat, lng, radiusKm = 10) {
    const response = await api.get(`/stations/nearby?lat=${lat}&lng=${lng}&radiusKm=${radiusKm}`);
    return response.data || response;
  }
};
