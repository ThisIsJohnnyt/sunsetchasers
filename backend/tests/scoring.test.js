const { calcScore, cloudRating, visibilityRating, conditionsRating, colorPotentialRating } = require('../src/services/scoring');

describe('Scoring Service', () => {
  describe('Rating Functions', () => {
    test('cloudRating calculates correctly', () => {
      expect(cloudRating(2)).toBe(0.65);
      expect(cloudRating(10)).toBe(0.95);
      expect(cloudRating(30)).toBe(1.0);
      expect(cloudRating(45)).toBe(0.85);
      expect(cloudRating(60)).toBe(0.7);
      expect(cloudRating(80)).toBe(0.4);
      expect(cloudRating(90)).toBe(0.1);
    });

    test('visibilityRating calculates correctly', () => {
      expect(visibilityRating(0.5)).toBe(0.0);
      expect(visibilityRating(2)).toBe(0.3);
      expect(visibilityRating(4)).toBe(0.7);
      expect(visibilityRating(7)).toBe(0.9);
      expect(visibilityRating(12)).toBe(1.0);
    });

    test('conditionsRating handles various conditions', () => {
      expect(conditionsRating('clear')).toBe(1.0);
      expect(conditionsRating('Clear')).toBe(1.0);
      expect(conditionsRating('clouds')).toBe(0.6);
      expect(conditionsRating('Cloudy')).toBe(0.6);
      expect(conditionsRating('rain')).toBe(0.05);
      expect(conditionsRating('thunderstorm')).toBe(0.0);
      expect(conditionsRating('unknown_condition')).toBe(0.5);
    });

    test('colorPotentialRating by altitude', () => {
      expect(colorPotentialRating(0)).toBe(1.0);
      expect(colorPotentialRating(-4)).toBe(0.95);
      expect(colorPotentialRating(-8)).toBe(0.75);
      expect(colorPotentialRating(-15)).toBe(0.5);
      expect(colorPotentialRating(-20)).toBe(0.2);
    });

    test('cloudRating handles edge cases', () => {
      expect(cloudRating(-10)).toBe(0.1);
      expect(cloudRating(150)).toBe(0.1);
      expect(cloudRating(0)).toBe(0.65);
      expect(cloudRating(100)).toBe(0.1);
    });

    test('visibilityRating handles negative values', () => {
      expect(visibilityRating(-5)).toBe(0.5);
    });
  });

  describe('Combined Scoring Algorithm', () => {
    test('Perfect conditions → Excellent', () => {
      const weather = {
        cloud_cover_percent: 25,
        visibility_km: 12,
        conditions: 'clear'
      };

      const result = calcScore(weather);

      expect(result.score).toBeGreaterThanOrEqual(0.95);
      expect(result.level).toBe(1);
      expect(result.label).toBe('Excellent');
      expect(result.breakdown.cloud_rating).toBe(1.0);
      expect(result.breakdown.visibility_rating).toBe(1.0);
      expect(result.breakdown.condition_rating).toBe(1.0);
      expect(result.breakdown.color_potential_rating).toBe(1.0);
    });

    test('Mediocre conditions → Good', () => {
      const weather = {
        cloud_cover_percent: 75,
        visibility_km: 5,
        conditions: 'overcast'
      };

      const result = calcScore(weather);

      expect(result.score).toBeGreaterThanOrEqual(0.60);
      expect(result.level).toBe(2);
      expect(result.label).toBe('Good');
    });

    test('Poor conditions → Poor', () => {
      const weather = {
        cloud_cover_percent: 98,
        visibility_km: 0.5,
        conditions: 'thunderstorm'
      };

      const result = calcScore(weather);

      expect(result.score).toBeLessThanOrEqual(0.40);
      expect(result.level).toBe(4);
      expect(result.label).toBe('Poor');
    });

    test('Fair conditions → Fair', () => {
      const weather = {
        cloud_cover_percent: 80,
        visibility_km: 2,
        conditions: 'overcast'
      };

      const result = calcScore(weather);

      expect(result.score).toBeGreaterThanOrEqual(0.40);
      expect(result.score).toBeLessThan(0.60);
      expect(result.level).toBe(3);
      expect(result.label).toBe('Fair');
    });

    test('Handles missing weather data gracefully', () => {
      const weather = {};

      expect(() => calcScore(weather)).not.toThrow();

      const result = calcScore(weather);
      expect(result).toHaveProperty('score');
      expect(result).toHaveProperty('level');
      expect(result).toHaveProperty('label');
      expect(result).toHaveProperty('reasoning');
    });

    test('Score is clamped between 0 and 1', () => {
      const perfectWeather = {
        cloud_cover_percent: 0,
        visibility_km: 50,
        conditions: 'clear'
      };

      const result = calcScore(perfectWeather);
      expect(result.score).toBeLessThanOrEqual(1.0);
      expect(result.score).toBeGreaterThanOrEqual(0.0);
    });

    test('Returns breakdown with all ratings', () => {
      const weather = {
        cloud_cover_percent: 30,
        visibility_km: 10,
        conditions: 'clear'
      };

      const result = calcScore(weather);

      expect(result.breakdown).toHaveProperty('cloud_rating');
      expect(result.breakdown).toHaveProperty('visibility_rating');
      expect(result.breakdown).toHaveProperty('condition_rating');
      expect(result.breakdown).toHaveProperty('color_potential_rating');

      Object.values(result.breakdown).forEach(rating => {
        expect(rating).toBeGreaterThanOrEqual(0);
        expect(rating).toBeLessThanOrEqual(1);
      });
    });

    test('Provides appropriate reasoning for each level', () => {
      const excellentWeather = {
        cloud_cover_percent: 20,
        visibility_km: 15,
        conditions: 'clear'
      };

      const poorWeather = {
        cloud_cover_percent: 95,
        visibility_km: 1,
        conditions: 'thunderstorm'
      };

      const excellentResult = calcScore(excellentWeather);
      const poorResult = calcScore(poorWeather);

      expect(excellentResult.reasoning).toContain('excellent');
      expect(poorResult.reasoning).toContain('Limited');
    });

    test('Weighted formula applies correctly', () => {
      const weatherA = {
        cloud_cover_percent: 15,
        visibility_km: 12,
        conditions: 'clear'
      };

      const weatherB = {
        cloud_cover_percent: 60,
        visibility_km: 12,
        conditions: 'clear'
      };

      const resultA = calcScore(weatherA);
      const resultB = calcScore(weatherB);

      expect(resultA.score).toBeGreaterThan(resultB.score);
    });
  });
});
