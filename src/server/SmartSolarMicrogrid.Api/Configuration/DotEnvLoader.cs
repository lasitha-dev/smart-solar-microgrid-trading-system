/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Defensive .env loader that discovers and parses local environment variable secret files.
 */

using System;
using System.IO;

namespace SmartSolarMicrogrid.Api.Configuration;

/// <summary>
/// Scans directory hierarchy and loads key-value pairs from .env files into process environment variables.
/// </summary>
public static class DotEnvLoader
{
    /// <summary>
    /// Searches for .env file starting from current directory up to parent levels.
    /// </summary>
    public static void Load()
    {
        var currentDir = new DirectoryInfo(Directory.GetCurrentDirectory());
        var envPath = FindEnvFile(currentDir) ?? FindEnvFile(new DirectoryInfo(AppContext.BaseDirectory));

        if (envPath == null || !File.Exists(envPath))
        {
            return;
        }

        try
        {
            var lines = File.ReadAllLines(envPath);
            foreach (var line in lines)
            {
                var trimmed = line.Trim();
                if (string.IsNullOrWhiteSpace(trimmed) || trimmed.StartsWith("#") || !trimmed.Contains('='))
                {
                    continue;
                }

                var parts = trimmed.Split('=', 2, StringSplitOptions.None);
                if (parts.Length != 2) continue;

                var key = parts[0].Trim();
                var value = parts[1].Trim();

                // Strip wrapping single or double quotes
                if ((value.StartsWith("\"") && value.EndsWith("\"")) ||
                    (value.StartsWith("'") && value.EndsWith("'")))
                {
                    value = value.Substring(1, value.Length - 2);
                }

                if (!string.IsNullOrEmpty(key))
                {
                    Environment.SetEnvironmentVariable(key, value);
                }
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[DotEnvLoader] Notice: Could not read .env file at {envPath}: {ex.Message}");
        }
    }

    private static string? FindEnvFile(DirectoryInfo? dir)
    {
        var depth = 0;
        while (dir != null && depth < 5)
        {
            var testPath = Path.Combine(dir.FullName, ".env");
            if (File.Exists(testPath))
            {
                return testPath;
            }

            dir = dir.Parent;
            depth++;
        }

        return null;
    }
}
