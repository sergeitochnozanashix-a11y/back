package software.pxel.learneasy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import software.pxel.learneasy.model.enums.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Профильные данные пользователя. Вынесены из {@link User} намеренно: User
 * реализует UserDetails и вычитывается целиком при валидации JWT на каждом
 * запросе, поэтому необязательные поля профиля там были бы мёртвым грузом.
 * <p>
 * Связь односторонняя. Обратная сторона {@code @OneToOne} в Hibernate всегда
 * загружается жадно (иначе не определить, null там или нет), так что маппинг
 * со стороны User вернул бы тот самый лишний запрос, которого мы избегаем.
 * Профиль читается явно через UserProfileRepository.
 * <p>
 * Строка создаётся лениво - при первом сохранении профиля. Её отсутствие
 * означает, что пользователь ещё ничего о себе не заполнил.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    /**
     * Совпадает с {@code users.id}: общий первичный ключ вместо суррогатного.
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    /**
     * Формат намеренно не навязывается: номера бывают международные,
     * с добавочными и без.
     */
    @Size(max = 32)
    @Column(name = "phone", length = 32)
    private String phone;

    @Size(max = 255)
    @Column(name = "address")
    private String address;

    @PastOrPresent
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 100)
    @Column(name = "country", length = 100)
    private String country;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    /**
     * objectKey, полученный из {@code POST /api/v1/file-storage/upload}.
     * Сам файл живёт в S3; клиент собирает адрес как
     * {@code GET /api/v1/file-storage/download/{avatarKey}}.
     */
    @Size(max = 512)
    @Column(name = "avatar_key", length = 512)
    private String avatarKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserProfile(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return userId != null && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserProfile{userId=" + userId + '}';
    }
}
