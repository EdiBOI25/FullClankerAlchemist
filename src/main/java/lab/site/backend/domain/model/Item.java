package lab.site.backend.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String emoji;

	private String iconUrl;

	@ManyToMany
	@JoinTable(name = "item_tag",
			joinColumns = @JoinColumn(name = "item_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
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
