/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Custom exception for business rule violations.
 */

namespace SmartSolarMicrogrid.Api.Exceptions
{
    public class BusinessRuleException : Exception
    {
        public string Code { get; }

        public BusinessRuleException(string code, string message) : base(message)
        {
            Code = code;
        }
    }

    public class NotFoundException : Exception
    {
        public NotFoundException(string message) : base(message)
        {
        }
    }
}
