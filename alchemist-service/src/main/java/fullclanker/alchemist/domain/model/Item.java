package fullclanker.alchemist.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Item {

	/** One of the items every user starts with, with its emoji and single tag. */
	public record Base(String name, String emoji, String tag) {
	}

	/** The items every user starts with; BaseItemSeeder creates them. */
	public static final List<Base> BASE_ITEMS = List.of(
			new Base("fire", "🔥", "hot"),
			new Base("water", "💧", "fluid"),
			new Base("earth", "🌍", "solid"),
			new Base("air", "💨", "gas"));

	public static final List<String> BASE_NAMES = BASE_ITEMS.stream().map(Base::name).toList();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, options = "COLLATE NOCASE")
	@NotBlank
	@Size(max = 50)
	private String name;

	@Size(max = 16)
	private String emoji;

	private String iconUrl;

	@ManyToMany
	@JoinTable(name = "item_tag",
			joinColumns = @JoinColumn(name = "item_id", options = "REFERENCES item (id)"),
			inverseJoinColumns = @JoinColumn(name = "tag_id", options = "REFERENCES tag (id)"))
	private Set<Tag> tags = new HashSet<>();

	protected Item() {
	}

	public Item(String name, String emoji, Set<Tag> tags) {
		this.name = name;
		this.emoji = emoji;
		this.tags = new HashSet<>(tags);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmoji() {
		return emoji;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public Set<Tag> getTags() {
		return tags;
	}

}
