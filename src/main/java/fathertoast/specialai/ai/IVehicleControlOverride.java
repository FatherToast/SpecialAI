package fathertoast.specialai.ai;

/**
 * Marker interface for goals that should stop their owner
 * from disabling movement-related control flags while they have
 * mob passengers, so the goal can keep running.
 */
public interface IVehicleControlOverride {
}
