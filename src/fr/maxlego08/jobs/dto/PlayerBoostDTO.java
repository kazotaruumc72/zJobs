package fr.maxlego08.jobs.dto;

import java.util.Date;
import java.util.UUID;

public record PlayerBoostDTO(int id, UUID unique_id, String job_id, String action_type, int remaining_boost, double experience_boost, double money_boost, Date created_at, Date updated_at) {
}
